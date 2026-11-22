package com.vesanrebackend.service.booking;

import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.booking.BookingDetail;
import com.vesanrebackend.dto.booking.BookingSummary;
import com.vesanrebackend.dto.coin.WalletView;
import com.vesanrebackend.entity.booking.Booking;
import com.vesanrebackend.entity.enums.BookingStatus;
import com.vesanrebackend.entity.enums.CancellationSource;
import com.vesanrebackend.entity.venue.Venue;
import com.vesanrebackend.repository.BookingRepository;
import com.vesanrebackend.repository.CourtRepository;
import com.vesanrebackend.repository.ShopRepository;
import com.vesanrebackend.repository.VenueRepository;
import com.vesanrebackend.service.coin.CoinWalletService;
import com.vesanrebackend.util.Paging;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/** Provider side: bookings of the caller's own shop, cancellation, shop wallet. */
@Service
public class ProviderBookingService {
    private final BookingRepository bookings;
    private final BookingLifecycle lifecycle;
    private final VenueRepository venues;
    private final CourtRepository courts;
    private final ShopRepository shops;
    private final CoinWalletService wallets;
    private final ObjectMapper json;

    public ProviderBookingService(BookingRepository bookings, BookingLifecycle lifecycle, VenueRepository venues,
                                  CourtRepository courts, ShopRepository shops, CoinWalletService wallets,
                                  ObjectMapper json) {
        this.bookings = bookings;
        this.lifecycle = lifecycle;
        this.venues = venues;
        this.courts = courts;
        this.shops = shops;
        this.wallets = wallets;
        this.json = json;
    }

    // ponytail: N+1 on block/court/venue per row; fetch-join if a page gets slow.
    @Transactional(readOnly = true)
    public PageResponse<BookingSummary> list(UUID userId, UUID venueId, UUID courtId, LocalDate date,
                                             BookingStatus status, int page, int size) {
        Specification<Booking> spec = (r, q, cb) ->
                cb.equal(r.get("scheduleBlock").get("court").get("venue").get("shop").get("owner").get("userId"), userId);
        if (venueId != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("scheduleBlock").get("court").get("venue").get("id"), venueId));
        }
        if (courtId != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("scheduleBlock").get("court").get("id"), courtId));
        }
        if (status != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        }
        if (date != null) {
            if (venueId == null && courtId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date filter requires venueId or courtId");
            }
            // The day is local to the venue; an unknown or foreign venue just yields an empty page.
            String tz = venueId != null
                    ? venues.findById(venueId).map(Venue::getTimezone).orElse("Asia/Ho_Chi_Minh")
                    : courts.findById(courtId).map(c -> c.getVenue().getTimezone()).orElse("Asia/Ho_Chi_Minh");
            ZoneId zone = ZoneId.of(tz);
            Instant from = date.atStartOfDay(zone).toInstant();
            Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();
            spec = spec.and((r, q, cb) -> cb.and(cb.greaterThanOrEqualTo(r.get("scheduleBlock").<Instant>get("startAt"), from),
                    cb.lessThan(r.get("scheduleBlock").<Instant>get("startAt"), to)));
        }
        return PageResponse.of(bookings.findAll(spec,
                Paging.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
                .map(BookingSummary::of));
    }

    @Transactional(readOnly = true)
    public BookingDetail get(UUID userId, UUID bookingId) {
        if (!bookings.existsForShopOwner(bookingId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        return BookingService.detail(bookings.findById(bookingId).orElseThrow(), json, true);
    }

    @Transactional
    public BookingDetail cancel(UUID userId, UUID bookingId, String reason) {
        // Boolean check only, same reason as BookingService.cancel.
        if (!bookings.existsForShopOwner(bookingId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        return BookingService.detail(lifecycle.cancel(bookingId, CancellationSource.PROVIDER, userId, reason), json, true);
    }

    @Transactional(readOnly = true)
    public WalletView wallet(UUID userId) {
        var shop = shops.findByOwnerUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));
        return new WalletView(wallets.shopBalance(shop.getId()));
    }
}
