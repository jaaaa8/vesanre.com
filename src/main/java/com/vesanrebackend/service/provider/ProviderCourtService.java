package com.vesanrebackend.service.provider;

import com.vesanrebackend.dto.provider.CourtDetailResponse;
import com.vesanrebackend.dto.provider.CreateCourtRequest;
import com.vesanrebackend.dto.provider.UpdateCourtRequest;
import com.vesanrebackend.dto.provider.CourtSportRequest;
import com.vesanrebackend.dto.provider.OperatingHourRequest;
import com.vesanrebackend.dto.provider.PricingRuleRequest;
import com.vesanrebackend.dto.provider.VenueAmenityRequest;
import com.vesanrebackend.entity.catalog.Amenity;
import com.vesanrebackend.entity.catalog.Sport;
import com.vesanrebackend.entity.court.Court;
import com.vesanrebackend.entity.court.CourtAmenity;
import com.vesanrebackend.entity.court.CourtOperatingHour;
import com.vesanrebackend.entity.court.CourtPricingRule;
import com.vesanrebackend.entity.court.CourtSport;
import com.vesanrebackend.entity.enums.AmenityScope;
import com.vesanrebackend.entity.venue.Venue;
import com.vesanrebackend.repository.AmenityRepository;
import com.vesanrebackend.repository.CourtAmenityRepository;
import com.vesanrebackend.repository.CourtOperatingHourRepository;
import com.vesanrebackend.repository.CourtPricingRuleRepository;
import com.vesanrebackend.repository.CourtRepository;
import com.vesanrebackend.repository.CourtSportRepository;
import com.vesanrebackend.repository.SportRepository;
import com.vesanrebackend.repository.VenueRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProviderCourtService {
    private final CourtRepository courts;
    private final VenueRepository venues;
    private final CourtSportRepository courtSports;
    private final CourtAmenityRepository courtAmenities;
    private final CourtOperatingHourRepository operatingHours;
    private final CourtPricingRuleRepository pricingRules;
    private final SportRepository sports;
    private final AmenityRepository amenities;
    private final ProviderImageService images;

    public ProviderCourtService(CourtRepository courts, VenueRepository venues, CourtSportRepository courtSports,
                                CourtAmenityRepository courtAmenities, CourtOperatingHourRepository operatingHours,
                                CourtPricingRuleRepository pricingRules, SportRepository sports,
                                AmenityRepository amenities, ProviderImageService images) {
        this.courts = courts;
        this.venues = venues;
        this.courtSports = courtSports;
        this.courtAmenities = courtAmenities;
        this.operatingHours = operatingHours;
        this.pricingRules = pricingRules;
        this.sports = sports;
        this.amenities = amenities;
        this.images = images;
    }

    @Transactional
    public CourtDetailResponse create(UUID userId, UUID venueId, CreateCourtRequest request) {
        Venue venue = venues.findOwned(venueId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));
        validateBooking(request.bookingStepMinutes(), request.minBookingMinutes(), request.maxBookingMinutes());
        Court court = new Court();
        court.setVenue(venue);
        court.setCode(required(request.code(), "code"));
        court.setName(required(request.name(), "name"));
        court.setDescription(blankToNull(request.description()));
        court.setCapacity(request.capacity());
        court.setBookingStepMinutes(request.bookingStepMinutes());
        court.setMinBookingMinutes(request.minBookingMinutes());
        court.setMaxBookingMinutes(request.maxBookingMinutes());
        courts.saveAndFlush(court); // duplicate (venue, code) surfaces here as 409
        return toDetail(court);
    }

    @Transactional(readOnly = true)
    public CourtDetailResponse get(UUID userId, UUID id) {
        return toDetail(owned(userId, id));
    }

    @Transactional
    public CourtDetailResponse update(UUID userId, UUID id, UpdateCourtRequest request) {
        Court court = owned(userId, id);
        // Re-check on merged values so a partial PATCH cannot break the V1 CHECK (which would be a 500).
        validateBooking(
                request.bookingStepMinutes() != null ? request.bookingStepMinutes() : court.getBookingStepMinutes(),
                request.minBookingMinutes() != null ? request.minBookingMinutes() : court.getMinBookingMinutes(),
                request.maxBookingMinutes() != null ? request.maxBookingMinutes() : court.getMaxBookingMinutes());
        if (request.code() != null) court.setCode(required(request.code(), "code"));
        if (request.name() != null) court.setName(required(request.name(), "name"));
        if (request.description() != null) court.setDescription(blankToNull(request.description()));
        if (request.capacity() != null) court.setCapacity(request.capacity());
        if (request.bookingStepMinutes() != null) court.setBookingStepMinutes(request.bookingStepMinutes());
        if (request.minBookingMinutes() != null) court.setMinBookingMinutes(request.minBookingMinutes());
        if (request.maxBookingMinutes() != null) court.setMaxBookingMinutes(request.maxBookingMinutes());
        if (request.status() != null) court.setStatus(request.status());
        courts.flush(); // duplicate code surfaces here as 409
        return toDetail(court);
    }

    // The replace-all PUTs: validate, bulk delete (flushes first and clears the persistence context, dropping the stale
    // child lists), insert, flush, then re-read the court so the response shows the new rows.
    @Transactional
    public CourtDetailResponse replaceSports(UUID userId, UUID id, List<CourtSportRequest> items) {
        Court court = owned(userId, id);
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one sport is required");
        }
        Set<UUID> ids = new HashSet<>();
        for (CourtSportRequest item : items) {
            if (!ids.add(item.sportId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate sportId " + item.sportId());
            }
        }
        if (items.stream().filter(CourtSportRequest::primary).count() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exactly one sport must be primary");
        }
        Map<UUID, Sport> found = sports.findAllById(ids).stream()
                .collect(Collectors.toMap(Sport::getId, Function.identity()));
        for (UUID sportId : ids) {
            Sport sport = found.get(sportId);
            if (sport == null || !sport.getIsActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown or inactive sport " + sportId);
            }
        }
        courtSports.deleteByCourtId(id);
        for (CourtSportRequest item : items) {
            CourtSport link = new CourtSport();
            link.setCourt(court);
            link.setSport(found.get(item.sportId()));
            link.getId().setCourtId(id);
            link.getId().setSportId(item.sportId());
            link.setIsPrimary(item.primary());
            courtSports.save(link);
        }
        courtSports.flush();
        return toDetail(owned(userId, id));
    }

    @Transactional
    public CourtDetailResponse replaceAmenities(UUID userId, UUID id, List<VenueAmenityRequest> items) {
        Court court = owned(userId, id);
        Set<UUID> ids = new HashSet<>();
        for (VenueAmenityRequest item : items) {
            if (!ids.add(item.amenityId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate amenityId " + item.amenityId());
            }
        }
        Map<UUID, Amenity> found = amenities.findAllById(ids).stream()
                .collect(Collectors.toMap(Amenity::getId, Function.identity()));
        for (UUID amenityId : ids) {
            Amenity amenity = found.get(amenityId);
            if (amenity == null || !amenity.getIsActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown or inactive amenity " + amenityId);
            }
            if (amenity.getAllowedScope() == AmenityScope.VENUE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Amenity " + amenity.getCode() + " is not allowed on a court");
            }
        }
        courtAmenities.deleteByCourtId(id);
        for (VenueAmenityRequest item : items) {
            CourtAmenity link = new CourtAmenity();
            link.setCourt(court);
            link.setAmenity(found.get(item.amenityId()));
            link.getId().setCourtId(id);
            link.getId().setAmenityId(item.amenityId());
            link.setDetails(blankToNull(item.details()));
            courtAmenities.save(link);
        }
        courtAmenities.flush();
        return toDetail(owned(userId, id));
    }

    @Transactional
    public CourtDetailResponse replaceOperatingHours(UUID userId, UUID id, List<OperatingHourRequest> items) {
        Court court = owned(userId, id);
        if (items.size() != 7 || items.stream().map(OperatingHourRequest::weekday).distinct().count() != 7) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exactly one entry for each weekday 0-6 is required");
        }
        for (OperatingHourRequest item : items) {
            if (item.closed()) {
                if (item.opensAt() != null || item.closesAt() != null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Weekday " + item.weekday() + " is closed and must not have opening hours");
                }
            } else if (item.opensAt() == null || item.closesAt() == null || !item.opensAt().isBefore(item.closesAt())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Weekday " + item.weekday() + " needs opensAt before closesAt");
            }
        }
        operatingHours.deleteByCourtId(id);
        for (OperatingHourRequest item : items) {
            CourtOperatingHour hour = new CourtOperatingHour();
            hour.setCourt(court);
            hour.getId().setCourtId(id);
            hour.getId().setWeekday(item.weekday().shortValue());
            hour.setIsClosed(item.closed());
            hour.setOpensAt(item.opensAt());
            hour.setClosesAt(item.closesAt());
            operatingHours.save(hour);
        }
        operatingHours.flush();
        return toDetail(owned(userId, id));
    }

    @Transactional
    public CourtDetailResponse replacePricingRules(UUID userId, UUID id, List<PricingRuleRequest> items) {
        Court court = owned(userId, id);
        for (PricingRuleRequest item : items) {
            if (item.startMinute() >= item.endMinute()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startMinute must be less than endMinute");
            }
        }
        // Per weekday, sorted by start: a rule starting before the previous end overlaps it.
        List<PricingRuleRequest> sorted = items.stream().sorted(Comparator.comparingInt(PricingRuleRequest::weekday)
                .thenComparingInt(PricingRuleRequest::startMinute)).toList();
        for (int i = 1; i < sorted.size(); i++) {
            PricingRuleRequest prev = sorted.get(i - 1);
            PricingRuleRequest cur = sorted.get(i);
            if (prev.weekday().equals(cur.weekday()) && cur.startMinute() < prev.endMinute()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Overlapping pricing rules on weekday " + cur.weekday());
            }
        }
        pricingRules.deleteByCourtId(id);
        for (PricingRuleRequest item : items) {
            CourtPricingRule rule = new CourtPricingRule();
            rule.setCourt(court);
            rule.setWeekday(item.weekday().shortValue());
            rule.setStartMinute(item.startMinute());
            rule.setEndMinute(item.endMinute());
            rule.setPricePerHour(item.pricePerHour());
            rule.setCurrency("VND");
            rule.setIsActive(true);
            pricingRules.save(rule);
        }
        pricingRules.flush(); // exclusion-constraint violation (last line of defence) surfaces here as 409
        return toDetail(owned(userId, id));
    }

    // Resolves a court the caller owns (404 otherwise).
    Court owned(UUID userId, UUID id) {
        return courts.findOwned(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Court not found"));
    }

    private static void validateBooking(int step, int min, int max) {
        if (min > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minBookingMinutes must not exceed maxBookingMinutes");
        }
        if (min % step != 0 || max % step != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "minBookingMinutes and maxBookingMinutes must be multiples of bookingStepMinutes");
        }
    }

    public CourtDetailResponse toDetail(Court c) {
        return new CourtDetailResponse(c.getId(), c.getVenue().getId(), c.getCode(), c.getName(), c.getDescription(),
                c.getCapacity(), c.getBookingStepMinutes(), c.getMinBookingMinutes(), c.getMaxBookingMinutes(),
                c.getStatus().name(),
                c.getSports().stream()
                        .map(s -> new CourtDetailResponse.SportItem(s.getSport().getId(), s.getSport().getCode(),
                                s.getSport().getName(), s.getIsPrimary()))
                        .sorted(Comparator.comparing(CourtDetailResponse.SportItem::code)).toList(),
                c.getAmenities().stream()
                        .map(a -> new CourtDetailResponse.AmenityItem(a.getAmenity().getId(), a.getAmenity().getCode(),
                                a.getAmenity().getName(), a.getDetails()))
                        .sorted(Comparator.comparing(CourtDetailResponse.AmenityItem::code)).toList(),
                c.getOperatingHours().stream()
                        .map(h -> new CourtDetailResponse.OperatingHourItem(h.getId().getWeekday(), h.getOpensAt(),
                                h.getClosesAt(), h.getIsClosed()))
                        .sorted(Comparator.comparingInt(CourtDetailResponse.OperatingHourItem::weekday)).toList(),
                c.getPricingRules().stream()
                        .map(r -> new CourtDetailResponse.PricingRuleItem(r.getId(), r.getWeekday(), r.getStartMinute(),
                                r.getEndMinute(), r.getPricePerHour(), r.getCurrency()))
                        .sorted(Comparator.comparingInt(CourtDetailResponse.PricingRuleItem::weekday)
                                .thenComparingInt(CourtDetailResponse.PricingRuleItem::startMinute)).toList(),
                images.ofCourt(c.getId()));
    }

    private static String required(String value, String field) {
        if (value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " cannot be blank");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
