package com.vesanrebackend.service.catalog;

import com.vesanrebackend.dto.catalog.CourtAvailabilityResponse;
import com.vesanrebackend.dto.catalog.PublicVenueDetailResponse;
import com.vesanrebackend.dto.review.RatingSummary;
import com.vesanrebackend.dto.provider.CourtDetailResponse;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Public (anonymous) read model: only ACTIVE venues, ACTIVE courts and active pricing rules. */
@Service
public class PublicCatalogService {
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
        // ponytail: N+1 per court collection; batch fetch if a venue gets many courts.
        List<PublicVenueDetailResponse.CourtItem> courtItems = courts.findByVenueIdOrderByCode(venueId).stream()
                .filter(c -> c.getStatus() == CourtStatus.ACTIVE).map(this::toItem).toList();
        BigDecimal fromPrice = courtItems.stream().flatMap(c -> c.pricingRules().stream())
                .map(CourtDetailResponse.PricingRuleItem::pricePerHour).min(Comparator.naturalOrder()).orElse(null);
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
                courtItems, fromPrice, ratingAverage, rating.count());
    }

    @Transactional(readOnly = true)
    public CourtAvailabilityResponse availability(UUID courtId, LocalDate date) {
        Court court = courts.findById(courtId)
                .filter(c -> c.getStatus() == CourtStatus.ACTIVE && c.getVenue().getStatus() == VenueStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Court not found"));
        ZoneId zone = ZoneId.of(court.getVenue().getTimezone());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        if (date.isBefore(today) || date.isAfter(today.plusDays(30))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date must be within the next 30 days");
        }
        int weekday = date.getDayOfWeek().getValue() - 1;
        CourtOperatingHour hours = court.getOperatingHours().stream()
                .filter(h -> h.getId().getWeekday().intValue() == weekday).findFirst().orElse(null);
        List<CourtPricingRule> rules = court.getPricingRules().stream()
                .filter(r -> r.getIsActive() && r.getWeekday().intValue() == weekday).toList();
        // Query the whole local day so a block starting the previous evening still counts.
        List<CourtScheduleBlock> active = blocks.findActiveOverlapping(courtId,
                date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant());
        return new CourtAvailabilityResponse(courtId, date, zone.getId(), hours == null || hours.getIsClosed(),
                court.getBookingStepMinutes(), court.getMinBookingMinutes(), court.getMaxBookingMinutes(),
                AvailabilityCalculator.slots(date, zone, hours, court.getBookingStepMinutes(), rules, active,
                        clock.instant()));
    }

    private PublicVenueDetailResponse.CourtItem toItem(Court c) {
        return new PublicVenueDetailResponse.CourtItem(c.getId(), c.getCode(), c.getName(), c.getDescription(),
                c.getCapacity(), c.getBookingStepMinutes(), c.getMinBookingMinutes(), c.getMaxBookingMinutes(),
                c.getSports().stream()
                        .map(s -> new CourtDetailResponse.SportItem(s.getSport().getId(), s.getSport().getCode(),
                                s.getSport().getName(), s.getIsPrimary()))
                        .sorted(Comparator.comparing(CourtDetailResponse.SportItem::code)).toList(),
                c.getAmenities().stream()
                        .map(a -> new CourtDetailResponse.AmenityItem(a.getAmenity().getId(), a.getAmenity().getCode(),
                                a.getAmenity().getName(), a.getDetails()))
                        .sorted(Comparator.comparing(CourtDetailResponse.AmenityItem::code)).toList(),
                List.of(), // ponytail: courts are hidden; the gallery is venue-level (images at the top level)
                c.getOperatingHours().stream()
                        .map(h -> new CourtDetailResponse.OperatingHourItem(h.getId().getWeekday(), h.getOpensAt(),
                                h.getClosesAt(), h.getIsClosed()))
                        .sorted(Comparator.comparingInt(CourtDetailResponse.OperatingHourItem::weekday)).toList(),
                c.getPricingRules().stream().filter(r -> r.getIsActive())
                        .map(r -> new CourtDetailResponse.PricingRuleItem(r.getId(), r.getWeekday(), r.getStartMinute(),
                                r.getEndMinute(), r.getPricePerHour(), r.getCurrency()))
                        .sorted(Comparator.comparingInt(CourtDetailResponse.PricingRuleItem::weekday)
                                .thenComparingInt(CourtDetailResponse.PricingRuleItem::startMinute)).toList());
    }
}
