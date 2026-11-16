package com.vesanrebackend.service.booking;

import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.booking.BookingDetail;
import com.vesanrebackend.dto.booking.BookingSummary;
import com.vesanrebackend.dto.booking.CreateBookingRequest;
import com.vesanrebackend.entity.booking.Booking;
import com.vesanrebackend.entity.coin.CoinWallet;
import com.vesanrebackend.entity.court.Court;
import com.vesanrebackend.entity.court.CourtOperatingHour;
import com.vesanrebackend.entity.court.CourtPricingRule;
import com.vesanrebackend.entity.court.CourtScheduleBlock;
import com.vesanrebackend.entity.enums.BookingStatus;
import com.vesanrebackend.entity.enums.CoinEntryType;
import com.vesanrebackend.entity.enums.CourtStatus;
import com.vesanrebackend.entity.enums.ScheduleBlockKind;
import com.vesanrebackend.entity.enums.VenueStatus;
import com.vesanrebackend.entity.shop.Shop;
import com.vesanrebackend.entity.venue.Venue;
import com.vesanrebackend.repository.BookingRepository;
import com.vesanrebackend.repository.CourtRepository;
import com.vesanrebackend.repository.CourtScheduleBlockRepository;
import com.vesanrebackend.repository.UserAccountRepository;
import com.vesanrebackend.service.catalog.AvailabilityCalculator;
import com.vesanrebackend.service.coin.CoinWalletService;
import com.vesanrebackend.util.Paging;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Customer side of bookings: create (deposit in coins), read, cancel. */
@Service
public class BookingService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final BookingRepository bookings;
    private final CourtRepository courts;
    private final CourtScheduleBlockRepository blocks;
    private final UserAccountRepository users;
    private final CoinWalletService wallets;
    private final BookingLifecycle lifecycle;
    private final ObjectMapper json;
    private final Clock clock;

    public BookingService(BookingRepository bookings, CourtRepository courts, CourtScheduleBlockRepository blocks,
                          UserAccountRepository users, CoinWalletService wallets, BookingLifecycle lifecycle,
                          ObjectMapper json, Clock clock) {
        this.bookings = bookings;
        this.courts = courts;
        this.blocks = blocks;
        this.users = users;
        this.wallets = wallets;
        this.lifecycle = lifecycle;
        this.json = json;
        this.clock = clock;
    }

    @Transactional
    public BookingDetail create(UUID customerId, CreateBookingRequest req) {
        Court court = courts.findById(req.courtId())
                .filter(c -> c.getStatus() == CourtStatus.ACTIVE && c.getVenue().getStatus() == VenueStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Court not found"));
        Venue venue = court.getVenue();
        Shop shop = venue.getShop();
        ZoneId zone = ZoneId.of(venue.getTimezone());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        if (req.date().isBefore(today) || req.date().isAfter(today.plusDays(30))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date must be within the next 30 days");
        }
        LocalTime start = req.startTime();
        LocalTime end = req.endTime();
        if (!end.isAfter(start)) { // also end = 00:00
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested time is not bookable");
        }
        long minutes = (end.toSecondOfDay() - start.toSecondOfDay()) / 60L;
        if (minutes < court.getMinBookingMinutes() || minutes > court.getMaxBookingMinutes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking duration is out of range");
        }
        int weekday = req.date().getDayOfWeek().getValue() - 1;
        CourtOperatingHour hours = court.getOperatingHours().stream()
                .filter(h -> h.getId().getWeekday().intValue() == weekday).findFirst().orElse(null);
        List<CourtPricingRule> rules = court.getPricingRules().stream()
                .filter(r -> r.getIsActive() && r.getWeekday().intValue() == weekday).toList();
        // Busy blocks are not passed: the DB exclusion constraint is the single authority on overlaps.
        var quote = BookingPricing.quote(AvailabilityCalculator.slots(req.date(), zone, hours,
                        court.getBookingStepMinutes(), rules, List.of(), clock.instant()), start, end,
                BookingPricing.depositPercent(shop.getDefaultCancellationPolicy(), json));

        CoinWallet wallet = wallets.lockUserWallet(customerId);
        if (wallet.getBalance() < quote.depositCoins()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient coins");
        }

        var customer = users.getReferenceById(customerId);
        CourtScheduleBlock block = new CourtScheduleBlock();
        block.setCourt(court);
        block.setBlockKind(ScheduleBlockKind.BOOKING);
        block.setCreatedBy(customer);
        block.setStartAt(req.date().atTime(start).atZone(zone).toInstant());
        block.setEndAt(req.date().atTime(end).atZone(zone).toInstant());
        Booking booking = new Booking();
        booking.setBookingCode(newCode());
        booking.setCustomer(customer);
        booking.setScheduleBlock(block);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(clock.instant());
        booking.setShopNameSnapshot(shop.getName());
        booking.setVenueNameSnapshot(venue.getName());
        booking.setCourtNameSnapshot(court.getName());
        booking.setUnitPriceSnapshot(quote.unitPrice());
        booking.setSubtotalAmount(quote.subtotal());
        booking.setDepositAmount(quote.depositAmount());
        booking.setDepositCoins(quote.depositCoins());
        booking.setCurrency("VND");
        booking.setPriceBreakdownSnapshot(json.writeValueAsString(quote.lines()));
        booking.setCancellationPolicySnapshot(json.writeValueAsString(
                Map.of("depositPercent", quote.depositPercent(), "depositReturnedOnCancel", true)));
        try {
            blocks.saveAndFlush(block);
            bookings.saveAndFlush(booking);
        } catch (DataAccessException e) {
            // 23P01 = exclusion_violation (overlapping ACTIVE block). 40P01 = deadlock: two concurrent inserts of
            // overlapping ranges wait on each other's exclusion check; Postgres aborts one, same meaning for us.
            if (e.getMostSpecificCause() instanceof SQLException sql
                    && ("23P01".equals(sql.getSQLState()) || "40P01".equals(sql.getSQLState()))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot is no longer available");
            }
            throw e;
        }
        wallets.apply(wallet, CoinEntryType.BOOKING_DEPOSIT, -quote.depositCoins(), booking, null);
        lifecycle.recordCreated(booking, customerId);
        return detail(booking, json, false);
    }

    // ponytail: N+1 on block/court/venue per row; fetch-join if a page gets slow.
    @Transactional(readOnly = true)
    public PageResponse<BookingSummary> list(UUID customerId, BookingStatus status, int page, int size) {
        Specification<Booking> spec = (r, q, cb) -> cb.equal(r.get("customer").get("id"), customerId);
        if (status != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        }
        return PageResponse.of(bookings.findAll(spec,
                Paging.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
                .map(BookingSummary::of));
    }

    @Transactional(readOnly = true)
    public BookingDetail get(UUID customerId, UUID bookingId) {
        return bookings.findById(bookingId).filter(b -> b.getCustomer().getId().equals(customerId))
                .map(b -> detail(b, json, false))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    public static BookingDetail detail(Booking b, ObjectMapper json, boolean withCustomer) {
        return BookingDetail.of(b, json, withCustomer);
    }

    private String newCode() {
        StringBuilder sb = new StringBuilder("BK").append(CODE_DATE.format(clock.instant().atOffset(ZoneOffset.UTC)));
        for (int i = 0; i < 6; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
