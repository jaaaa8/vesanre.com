package com.vesanrebackend.service.review;

import com.vesanrebackend.dto.review.CreateReviewRequest;
import com.vesanrebackend.dto.review.ReviewView;
import com.vesanrebackend.entity.booking.Booking;
import com.vesanrebackend.entity.booking.Review;
import com.vesanrebackend.entity.enums.BookingStatus;
import com.vesanrebackend.entity.shop.Shop;
import com.vesanrebackend.repository.BookingRepository;
import com.vesanrebackend.repository.NotificationRepository;
import com.vesanrebackend.repository.ReviewRepository;
import com.vesanrebackend.service.mail.ReviewMailer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final BookingRepository bookings;
    private final NotificationRepository notifications;
    private final ReviewMailer mailer;

    public ReviewService(ReviewRepository reviews, BookingRepository bookings, NotificationRepository notifications,
                         ReviewMailer mailer) {
        this.reviews = reviews;
        this.bookings = bookings;
        this.notifications = notifications;
        this.mailer = mailer;
    }

    @Transactional
    public ReviewView create(UUID customerId, UUID bookingId, CreateReviewRequest req) {
        Booking booking = bookings.findById(bookingId).filter(b -> b.getCustomer().getId().equals(customerId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking is not completed");
        }
        if (reviews.existsByBookingId(bookingId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking already reviewed");
        }
        Review review = new Review();
        review.setBooking(booking);
        review.setCustomer(booking.getCustomer());
        review.setRating(req.rating().shortValue());
        review.setComment(req.comment());
        review = reviews.saveAndFlush(review); // a concurrent duplicate hits uq_reviews_booking -> 409

        Shop shop = booking.getScheduleBlock().getCourt().getVenue().getShop();
        String venueName = booking.getScheduleBlock().getCourt().getVenue().getName();
        String body = booking.getCustomer().getDisplayName() + " chấm " + req.rating() + " sao cho " + venueName
                + " (" + booking.getBookingCode() + ")";
        notifications.insertIfAbsent(shop.getOwner().getUserId(), bookingId, "REVIEW_CREATED", "Đánh giá mới", body,
                "review:" + review.getId());
        mailer.send(shop.getOwner().getUser().getEmail(), "[Vesanre] Đánh giá mới cho " + venueName, body,
                null);
        return ReviewView.of(review);
    }
}
