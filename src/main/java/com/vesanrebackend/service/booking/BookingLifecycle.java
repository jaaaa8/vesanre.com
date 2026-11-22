package com.vesanrebackend.service.booking;

import com.vesanrebackend.entity.OutboxEvent;
import com.vesanrebackend.entity.booking.Booking;
import com.vesanrebackend.entity.booking.BookingStatusHistory;
import com.vesanrebackend.entity.coin.CoinWallet;
import com.vesanrebackend.entity.enums.BookingActorType;
import com.vesanrebackend.entity.enums.BookingStatus;
import com.vesanrebackend.entity.enums.CancellationSource;
import com.vesanrebackend.entity.enums.CoinEntryType;
import com.vesanrebackend.entity.enums.ScheduleBlockState;
import com.vesanrebackend.repository.BookingRepository;
import com.vesanrebackend.repository.BookingStatusHistoryRepository;
import com.vesanrebackend.repository.OutboxEventRepository;
import com.vesanrebackend.repository.UserAccountRepository;
import com.vesanrebackend.service.coin.CoinWalletService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Status transitions: every one writes a history row and an outbox event in the caller's transaction. */
@Service
public class BookingLifecycle {
    private final BookingRepository bookings;
    private final BookingStatusHistoryRepository history;
    private final OutboxEventRepository outbox;
    private final UserAccountRepository users;
    private final CoinWalletService wallets;
    private final ObjectMapper json;
    private final Clock clock;

    public BookingLifecycle(BookingRepository bookings, BookingStatusHistoryRepository history,
                            OutboxEventRepository outbox, UserAccountRepository users, CoinWalletService wallets,
                            ObjectMapper json, Clock clock) {
        this.bookings = bookings;
        this.history = history;
        this.outbox = outbox;
        this.users = users;
        this.wallets = wallets;
        this.json = json;
        this.clock = clock;
    }

    @Transactional
    public void recordCreated(Booking booking, UUID actorUserId) {
        record(booking, null, BookingStatus.CONFIRMED, BookingActorType.CUSTOMER, actorUserId, "BOOKED",
                booking.getDepositCoins());
    }

    /** Lock order (spec section 6): booking, then user wallet, then the block is released. */
    @Transactional
    public Booking cancel(UUID bookingId, CancellationSource source, UUID actorUserId, String reason) {
        Booking b = bookings.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking is " + b.getStatus());
        }
        Instant now = clock.instant();
        var block = b.getScheduleBlock();
        if (!now.isBefore(block.getStartAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking already started");
        }
        b.setStatus(BookingStatus.CANCELLED);
        b.setCancelledAt(now);
        b.setCancellationSource(source);
        b.setCancelledBy(users.getReferenceById(actorUserId));
        b.setCancellationReason(reason);
        CoinWallet wallet = wallets.lockUserWallet(b.getCustomer().getId());
        wallets.apply(wallet, CoinEntryType.BOOKING_DEPOSIT_RETURN, b.getDepositCoins(), b, null);
        block.setState(ScheduleBlockState.RELEASED);
        block.setReleasedAt(now);
        boolean byCustomer = source == CancellationSource.CUSTOMER;
        record(b, BookingStatus.CONFIRMED, BookingStatus.CANCELLED,
                byCustomer ? BookingActorType.CUSTOMER : BookingActorType.PROVIDER, actorUserId,
                byCustomer ? "CANCELLED_BY_CUSTOMER" : "CANCELLED_BY_PROVIDER", b.getDepositCoins());
        return b;
    }

    /** Pays the deposit into the shop wallet once the booking has ended. Lock order: booking, then shop wallet. */
    @Transactional
    public void complete(UUID bookingId) {
        Booking b = bookings.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking is " + b.getStatus());
        }
        Instant now = clock.instant();
        var block = b.getScheduleBlock();
        if (block.getEndAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking has not ended");
        }
        b.setStatus(BookingStatus.COMPLETED);
        b.setCompletedAt(now);
        CoinWallet shopWallet = wallets.lockShopWallet(block.getCourt().getVenue().getShop().getId());
        wallets.apply(shopWallet, CoinEntryType.BOOKING_PAYOUT, b.getDepositCoins(), b, null);
        record(b, BookingStatus.CONFIRMED, BookingStatus.COMPLETED, BookingActorType.SYSTEM, null, "COMPLETED",
                b.getDepositCoins());
    }

    private void record(Booking b, BookingStatus from, BookingStatus to, BookingActorType actor, UUID actorUserId,
                        String reasonCode, long coins) {
        BookingStatusHistory h = new BookingStatusHistory();
        h.setBooking(b);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setActorType(actor);
        h.setActorUser(actorUserId == null ? null : users.getReferenceById(actorUserId));
        h.setReasonCode(reasonCode);
        h.setMetadata(json.writeValueAsString(Map.of("coins", coins)));
        history.save(h);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookingId", b.getId());
        payload.put("bookingCode", b.getBookingCode());
        payload.put("fromStatus", from);
        payload.put("toStatus", to);
        payload.put("coins", coins);
        OutboxEvent e = new OutboxEvent();
        e.setEventKey(b.getId() + ":" + to);
        e.setAggregateType("BOOKING");
        e.setAggregateId(b.getId());
        e.setEventType("BOOKING_" + to);
        e.setPayload(json.writeValueAsString(payload));
        e.setNextAttemptAt(clock.instant());
        outbox.save(e);
    }
}
