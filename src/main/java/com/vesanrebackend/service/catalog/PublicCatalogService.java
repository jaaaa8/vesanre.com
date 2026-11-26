package com.vesanrebackend.service.catalog;

import com.vesanrebackend.dto.catalog.CourtAvailabilityResponse;
import com.vesanrebackend.dto.catalog.PublicVenueDetailResponse;
import com.vesanrebackend.dto.catalog.VenueAvailabilityResponse;
import com.vesanrebackend.dto.review.RatingSummary;
import com.vesanrebackend.dto.provider.PricingRuleRequest;
import com.vesanrebackend.dto.provider.VenueDetailResponse;
import com.vesanrebackend.entity.court.Court;
import com.vesanrebackend.entity.court.CourtOperatingHour;
import com.vesanrebackend.entity.court.CourtPricingRule;
import com.vesanrebackend.entity.court.CourtScheduleBlock;
import com.vesanrebackend.entity.enums.CourtStatus;
import com.vesanrebackend.entity.enums.VenueStatus;
import com.vesanrebackend.entity.venue.Venue;
import com.vesanrebackend.repository.CourtRepository;
import com.vesanrebackend.repository.CourtScheduleBlockRepository;
import com.vesanrebackend.repository.ReviewRepository;
import com.vesanrebackend.repository.VenueAmenityRepository;
import com.vesanrebackend.repository.VenueRepository;
import com.vesanrebackend.service.provider.ProviderImageService;
import com.vesanrebackend.service.storage.ImageStorage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Public (anonymous) read model: only ACTIVE venues, ACTIVE courts and active pricing rules. */
@Service
public class PublicCatalogService {
    // S1, S2, ..., S10: by code length then code, same as CourtRepository.lockActiveByVenue.
    private static final Comparator<Court> COURT_ORDER = Comparator.comparingInt((Court c) -> c.getCode().length())
            .thenComparing(Court::getCode);

    private final VenueRepository venues;
    private final CourtRepository courts;
    private final VenueAmenityRepository venueAmenities;
    private final CourtScheduleBlockRepository blocks;
    private final ProviderImageService images;
    private final ImageStorage storage;
    private final ReviewRepository reviews;
    private final Clock clock;

    public PublicCatalogService(VenueRepository venues, CourtRepository courts, VenueAmenityRepository venueAmenities,
                                CourtScheduleBlockRepository blocks, ProviderImageService images, ImageStorage storage,
                                ReviewRepository reviews, Clock clock) {
        this.venues = venues;
        this.courts = courts;
        this.venueAmenities = venueAmenities;
        this.blocks = blocks;
        this.images = images;
        this.storage = storage;
        this.reviews = reviews;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PublicVenueDetailResponse detail(UUID venueId) {
        Venue v = venues.findById(venueId).filter(x -> x.getStatus() == VenueStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));
        List<Court> active = activeCourts(venueId);
        // Courts are identical, so the first one speaks for the venue.
        List<PricingRuleRequest> rules = active.isEmpty() ? List.of() : active.get(0).getPricingRules().stream()
                .filter(r -> r.getIsActive())
                .map(r -> new PricingRuleRequest(r.getWeekday().intValue(), r.getStartMinute(), r.getEndMinute(),
                        r.getPricePerHour()))
                .sorted(Comparator.comparingInt(PricingRuleRequest::weekday)
                        .thenComparingInt(PricingRuleRequest::startMinute)).toList();
        String sport = active.isEmpty() ? null : active.get(0).getSports().stream()
                .map(s -> s.getSport().getName()).findFirst().orElse(null);
        BigDecimal fromPrice = rules.stream().map(PricingRuleRequest::pricePerHour).min(Comparator.naturalOrder())
                .orElse(null);
        RatingSummary rating = reviews.ratingOf(venueId);
        BigDecimal ratingAverage = rating.count() == 0 ? null
                : BigDecimal.valueOf(rating.average()).setScale(1, RoundingMode.HALF_UP);
        return new PublicVenueDetailResponse(v.getId(), v.getName(), v.getDescription(), v.getAddressLine(),
                v.getWard(), v.getDistrict(), v.getCity(), v.getProvince(), v.getLatitude(), v.getLongitude(),
                v.getPhone(), new PublicVenueDetailResponse.ShopItem(v.getShop().getName(),
                        storage.url(v.getShop().getLogoStorageKey())),
                images.ofVenue(venueId),
                venueAmenities.findByVenueId(venueId).stream()
                        .map(a -> new VenueDetailResponse.AmenityItem(a.getAmenity().getId(), a.getAmenity().getCode(),
                                a.getAmenity().getName(), a.getDetails())).toList(),
                sport, active.size(), v.getDepositPercent(), rules, fromPrice, ratingAverage, rating.count());
    }

    @Transactional(readOnly = true)
    public VenueAvailabilityResponse venueAvailability(UUID venueId, LocalDate date) {
        Venue venue = venues.findById(venueId).filter(x -> x.getStatus() == VenueStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));
        List<Court> active = activeCourts(venueId);
        if (active.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue has no courts");
        }
        ZoneId zone = ZoneId.of(venue.getTimezone());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        if (date.isBefore(today) || date.isAfter(today.plusDays(30))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date must be within the next 30 days");
        }
        int weekday = date.getDayOfWeek().getValue() - 1;
        List<List<CourtAvailabilityResponse.Slot>> perCourt = new ArrayList<>();
        boolean closed = false;
        for (Court court : active) {
            CourtOperatingHour hours = court.getOperatingHours().stream()
                    .filter(h -> h.getId().getWeekday().intValue() == weekday).findFirst().orElse(null);
            if (perCourt.isEmpty()) {
                closed = hours == null || hours.getIsClosed();
            }
            List<CourtPricingRule> rules = court.getPricingRules().stream()
                    .filter(r -> r.getIsActive() && r.getWeekday().intValue() == weekday).toList();
            // Query the whole local day so a block starting the previous evening still counts.
            List<CourtScheduleBlock> busy = blocks.findActiveOverlapping(court.getId(),
                    date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant());
            perCourt.add(AvailabilityCalculator.slots(date, zone, hours, court.getBookingStepMinutes(), rules, busy,
                    clock.instant()));
        }
        return new VenueAvailabilityResponse(venueId, date, closed, active.size(), venue.getDepositPercent(),
                VenueSlots.merge(perCourt));
    }

    private List<Court> activeCourts(UUID venueId) {
        return courts.findByVenueIdOrderByCode(venueId).stream().filter(c -> c.getStatus() == CourtStatus.ACTIVE)
                .sorted(COURT_ORDER).toList();
    }
}
