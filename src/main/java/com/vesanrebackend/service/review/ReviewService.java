package com.vesanrebackend.service.review;

import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.review.CreateReviewRequest;
import com.vesanrebackend.dto.review.ProviderReviewItem;
import com.vesanrebackend.dto.review.ReviewView;
import com.vesanrebackend.entity.booking.Booking;
import com.vesanrebackend.entity.booking.Review;
import com.vesanrebackend.entity.enums.BookingStatus;
import com.vesanrebackend.entity.enums.ReviewStatus;
import com.vesanrebackend.entity.enums.VenueStatus;
import com.vesanrebackend.entity.shop.Shop;
import com.vesanrebackend.repository.BookingRepository;
import com.vesanrebackend.repository.NotificationRepository;
import com.vesanrebackend.repository.ReviewRepository;
import com.vesanrebackend.repository.VenueRepository;
import com.vesanrebackend.service.mail.ReviewMailer;
import com.vesanrebackend.util.Paging;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.UUID;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final BookingRepository bookings;
    private final NotificationRepository notifications;
    private final ReviewMailer mailer;
    private final VenueRepository venues;
    private final Clock clock;

    public ReviewService(ReviewRepository reviews, BookingRepository bookings, NotificationRepository notifications,
                         ReviewMailer mailer, VenueRepository venues, Clock clock) {
        this.reviews = reviews;
        this.bookings = bookings;
        this.notifications = notifications;
        this.mailer = mailer;
        this.venues = venues;
        this.clock = clock;
    }

    private static Pageable newestFirst(int page, int size) {
        return Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewView> publicList(UUID venueId, int page, int size) {
        venues.findById(venueId).filter(v -> v.getStatus() == VenueStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));
        return PageResponse.of(reviews.findByVenueAndStatus(venueId, ReviewStatus.PUBLISHED, newestFirst(page, size))
                .map(ReviewView::of));
    }

    // ponytail: N+1 on venue/booking per row; fetch join if pages get large.
    @Transactional(readOnly = true)
    public PageResponse<ProviderReviewItem> providerList(UUID ownerId, int page, int size) {
        return PageResponse.of(reviews.findByShopOwner(ownerId, newestFirst(page, size)).map(ProviderReviewItem::of));
    }

    @Transactional
    public ReviewView reply(UUID ownerId, UUID reviewId, String reply) {
        if (!reviews.existsOwned(reviewId, ownerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found");
        }
        if (reviews.reply(reviewId, reply, ownerId, clock.instant()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review already replied");
        }
        return ReviewView.of(reviews.findById(reviewId).orElseThrow());
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
