package com.vesanrebackend.service.booking;

import com.vesanrebackend.entity.OutboxEvent;
import com.vesanrebackend.entity.booking.Booking;
import com.vesanrebackend.entity.booking.BookingStatusHistory;
import com.vesanrebackend.entity.enums.BookingActorType;
import com.vesanrebackend.entity.enums.BookingStatus;
import com.vesanrebackend.repository.BookingRepository;
import com.vesanrebackend.repository.BookingStatusHistoryRepository;
import com.vesanrebackend.repository.OutboxEventRepository;
import com.vesanrebackend.repository.UserAccountRepository;
import com.vesanrebackend.service.coin.CoinWalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
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
