package com.vesanrebackend.service.notification;

import com.vesanrebackend.entity.OutboxEvent;
import com.vesanrebackend.entity.account.UserAccount;
import com.vesanrebackend.entity.booking.Booking;
import com.vesanrebackend.repository.BookingRepository;
import com.vesanrebackend.repository.NotificationRepository;
import com.vesanrebackend.service.mail.ReviewMailer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/** Turns one booking event into notifications (+ mail). Own transaction so a failure leaves no partial rows. */
@Component
public class OutboxHandler {
    private final BookingRepository bookings;
    private final NotificationRepository notifications;
    private final ReviewMailer mailer;
    private final ObjectMapper json;

    public OutboxHandler(BookingRepository bookings, NotificationRepository notifications, ReviewMailer mailer,
                         ObjectMapper json) {
        this.bookings = bookings;
        this.notifications = notifications;
        this.mailer = mailer;
        this.json = json;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(OutboxEvent e) {
        var payload = json.readTree(e.getPayload());
        UUID bookingId = UUID.fromString(payload.get("bookingId").asString());
        long coins = payload.path("coins").asLong(0);
        Booking b = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking " + bookingId + " not found"));
        String where = b.getVenueNameSnapshot() + " - " + b.getCourtNameSnapshot();
        String code = b.getBookingCode();

        String title;
        String customerBody;
        String ownerBody = null;
        switch (e.getEventType()) {
            case "BOOKING_CONFIRMED" -> {
                title = "Đặt sân thành công " + code;
                customerBody = "Đã xác nhận " + code + " tại " + where + " — đã trừ " + coins + " coin cọc.";
                ownerBody = "Có đặt sân mới " + code + " tại " + where + ".";
            }
            case "BOOKING_CANCELLED" -> {
                title = "Đã hủy " + code;
                customerBody = "Đã hủy " + code + " tại " + where + " — " + coins + " coin đã trả về ví.";
                ownerBody = "Booking " + code + " tại " + where + " đã bị hủy.";
            }
            case "BOOKING_COMPLETED" -> {
                title = "Hoàn tất " + code;
                customerBody = "Booking " + code + " tại " + where + " đã hoàn tất.";
            }
            default -> {
                title = "Cập nhật booking " + code;
                customerBody = "Booking " + code + " có cập nhật mới.";
            }
        }
        notify(e, b, b.getCustomer(), title, customerBody);
        if (ownerBody != null) {
            notify(e, b, b.getScheduleBlock().getCourt().getVenue().getShop().getOwner().getUser(), title, ownerBody);
        }
    }

    // Mail only for a fresh notification, so a redelivered event never mails twice.
    private void notify(OutboxEvent e, Booking b, UserAccount user, String title, String body) {
        int inserted = notifications.insertIfAbsent(user.getId(), b.getId(), e.getEventType(), title, body,
                e.getEventKey() + ":" + user.getId());
        if (inserted == 1) {
            mailer.send(user.getEmail(), title, body, null);
        }
    }
}
