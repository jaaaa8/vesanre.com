package com.vesanrebackend.service.provider;

import com.vesanrebackend.dto.provider.OperatingHourRequest;
import com.vesanrebackend.dto.provider.PricingRuleRequest;
import com.vesanrebackend.dto.provider.VenueSetupRequest;
import com.vesanrebackend.dto.provider.VenueSetupResponse;
import com.vesanrebackend.entity.catalog.Sport;
import com.vesanrebackend.entity.court.Court;
import com.vesanrebackend.entity.court.CourtOperatingHour;
import com.vesanrebackend.entity.court.CourtPricingRule;
import com.vesanrebackend.entity.court.CourtSport;
import com.vesanrebackend.entity.enums.CourtStatus;
import com.vesanrebackend.entity.venue.Venue;
import com.vesanrebackend.repository.CourtOperatingHourRepository;
import com.vesanrebackend.repository.CourtPricingRuleRepository;
import com.vesanrebackend.repository.CourtRepository;
import com.vesanrebackend.repository.CourtScheduleBlockRepository;
import com.vesanrebackend.repository.CourtSportRepository;
import com.vesanrebackend.repository.SportRepository;
import com.vesanrebackend.repository.VenueRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Venue-level setup: a venue owns N identical hidden courts (S1..Sn) that all carry the same sport, hours and prices.
 * The provider only ever sees and sends the venue-level config; courts are synced to it here.
 */
@Service
public class VenueSetupService {
    private final VenueRepository venues;
    private final CourtRepository courts;
    private final CourtScheduleBlockRepository blocks;
    private final CourtSportRepository courtSports;
    private final CourtOperatingHourRepository operatingHours;
    private final CourtPricingRuleRepository pricingRules;
    private final SportRepository sports;
    private final EntityManager em;
    private final Clock clock;

    public VenueSetupService(VenueRepository venues, CourtRepository courts, CourtScheduleBlockRepository blocks,
                             CourtSportRepository courtSports, CourtOperatingHourRepository operatingHours,
                             CourtPricingRuleRepository pricingRules, SportRepository sports, EntityManager em,
                             Clock clock) {
        this.venues = venues;
        this.courts = courts;
        this.blocks = blocks;
        this.courtSports = courtSports;
        this.operatingHours = operatingHours;
        this.pricingRules = pricingRules;
        this.sports = sports;
        this.em = em;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public VenueSetupResponse get(UUID userId, UUID venueId) {
        owned(userId, venueId);
        return read(venueId);
    }

    /** No ownership check: for admin. Config comes from the first ACTIVE court. */
    @Transactional(readOnly = true)
    public VenueSetupResponse read(UUID venueId) {
        List<Court> active = courts.findByVenueIdOrderByCode(venueId).stream()
                .filter(c -> c.getStatus() == CourtStatus.ACTIVE).sorted(Comparator.comparingInt(VenueSetupService::n))
                .toList();
        int deposit = venues.findById(venueId).map(v -> (int) v.getDepositPercent()).orElse(0);
        if (active.isEmpty()) {
            return new VenueSetupResponse(null, 0, deposit, List.of(), List.of());
        }
        Court c = active.get(0);
        return new VenueSetupResponse(
                c.getSports().stream().findFirst().map(s -> s.getSport().getCode()).orElse(null), active.size(), deposit,
                c.getOperatingHours().stream()
                        .sorted(Comparator.comparingInt(h -> h.getId().getWeekday()))
                        .map(h -> new OperatingHourRequest((int) h.getId().getWeekday(), h.getOpensAt(), h.getClosesAt(),
                                h.getIsClosed())).toList(),
                c.getPricingRules().stream().filter(CourtPricingRule::getIsActive)
                        .sorted(Comparator.comparingInt((CourtPricingRule r) -> r.getWeekday())
                                .thenComparingInt(CourtPricingRule::getStartMinute))
                        .map(r -> new PricingRuleRequest((int) r.getWeekday(), r.getStartMinute(), r.getEndMinute(),
                                r.getPricePerHour())).toList());
    }

    @Transactional
    public VenueSetupResponse replace(UUID userId, UUID venueId, VenueSetupRequest req) {
        Venue venue = owned(userId, venueId);
        List<Court> active = new ArrayList<>(courts.lockActiveByVenue(venueId)); // lock first, same order as booking
        Sport sport = validate(req);

        int want = req.courtCount();
        if (active.size() < want) {
            List<Court> all = courts.findByVenueIdOrderByCode(venueId);
            all.stream().filter(c -> c.getStatus() == CourtStatus.INACTIVE).sorted(Comparator.comparingInt(VenueSetupService::n))
                    .limit(want - active.size()).forEach(c -> {
                        c.setStatus(CourtStatus.ACTIVE);
                        active.add(c);
                    });
            int next = all.stream().mapToInt(VenueSetupService::n).max().orElse(0) + 1;
            while (active.size() < want) {
                active.add(newCourt(venue, next++));
            }
        } else if (active.size() > want) {
            List<Court> remove = new ArrayList<>();
            for (int i = active.size() - 1; i >= 0 && remove.size() < active.size() - want; i--) {
                if (!blocks.existsFutureBooking(active.get(i).getId(), clock.instant())) {
                    remove.add(active.get(i));
                }
            }
            if (remove.size() < active.size() - want) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Upcoming bookings on courts to remove");
            }
            remove.forEach(c -> c.setStatus(CourtStatus.INACTIVE));
            active.removeAll(remove);
        }
        venue.setDepositPercent((short) req.depositPercent());
        courts.flush();

        // ponytail: per-court delete + insert, up to 50 courts x 3 tables; copy-on-write only if setup gets hot.
        for (Court court : active) {
            UUID id = court.getId();
            courtSports.deleteByCourtId(id);
            operatingHours.deleteByCourtId(id);
            pricingRules.deleteByCourtId(id);
            CourtSport link = new CourtSport();
            link.setCourt(court);
            link.setSport(sport);
            link.getId().setCourtId(id);
            link.getId().setSportId(sport.getId());
            link.setIsPrimary(true);
            courtSports.save(link);
            for (OperatingHourRequest item : req.operatingHours()) {
                CourtOperatingHour hour = new CourtOperatingHour();
                hour.setCourt(court);
                hour.getId().setCourtId(id);
                hour.getId().setWeekday(item.weekday().shortValue());
                hour.setIsClosed(item.closed());
                hour.setOpensAt(item.opensAt());
                hour.setClosesAt(item.closesAt());
                operatingHours.save(hour);
            }
            for (PricingRuleRequest item : req.pricingRules()) {
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
        }
        courts.flush();
        em.clear(); // new Court objects hold empty child lists; re-read fresh from the DB
        return read(venueId);
    }

    private Court newCourt(Venue venue, int n) {
        Court court = new Court();
        court.setVenue(venue);
        court.setCode("S" + n);
        court.setName("Court S" + n);
        court.setCapacity(4);
        court.setBookingStepMinutes(60);
        court.setMinBookingMinutes(60);
        court.setMaxBookingMinutes(120);
        return courts.saveAndFlush(court);
    }

    private Sport validate(VenueSetupRequest req) {
        Sport sport = sports.findByCodeAndIsActiveTrue(req.sportCode())
                .orElseThrow(() -> bad("Unknown or inactive sport " + req.sportCode()));
        List<OperatingHourRequest> hours = req.operatingHours();
        if (hours.size() != 7 || hours.stream().map(OperatingHourRequest::weekday).distinct().count() != 7) {
            throw bad("Exactly one entry for each weekday 0-6 is required");
        }
        for (OperatingHourRequest h : hours) {
            if (h.closed()) {
                if (h.opensAt() != null || h.closesAt() != null) {
                    throw bad("Weekday " + h.weekday() + " is closed and must not have opening hours");
                }
            } else if (h.opensAt() == null || h.closesAt() == null || !h.opensAt().isBefore(h.closesAt())) {
                throw bad("Weekday " + h.weekday() + " needs opensAt before closesAt");
            } else if (!wholeHour(h.opensAt()) || !wholeHour(h.closesAt())) {
                throw bad("Weekday " + h.weekday() + " hours must be whole hours");
            }
        }
        for (PricingRuleRequest r : req.pricingRules()) {
            if (r.startMinute() >= r.endMinute()) {
                throw bad("startMinute must be less than endMinute");
            }
            if (r.startMinute() % 60 != 0 || r.endMinute() % 60 != 0) {
                throw bad("Pricing rule minutes must be whole hours");
            }
        }
        // Per weekday, sorted by start: a rule starting before the previous end overlaps it.
        List<PricingRuleRequest> sorted = req.pricingRules().stream().sorted(
                Comparator.comparingInt(PricingRuleRequest::weekday).thenComparingInt(PricingRuleRequest::startMinute)).toList();
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i - 1).weekday().equals(sorted.get(i).weekday())
                    && sorted.get(i).startMinute() < sorted.get(i - 1).endMinute()) {
                throw bad("Overlapping pricing rules on weekday " + sorted.get(i).weekday());
            }
        }
        return sport;
    }

    private static boolean wholeHour(LocalTime t) {
        return t.equals(t.truncatedTo(ChronoUnit.HOURS));
    }

    private static int n(Court c) {
        return Integer.parseInt(c.getCode().substring(1));
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private Venue owned(UUID userId, UUID venueId) {
        return venues.findOwned(venueId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));
    }
}
