package com.vesanrebackend.controller.booking;

import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.booking.BookingDetail;
import com.vesanrebackend.dto.booking.BookingSummary;
import com.vesanrebackend.dto.booking.CancelBookingRequest;
import com.vesanrebackend.dto.booking.CreateBookingRequest;
import com.vesanrebackend.entity.enums.BookingStatus;
import com.vesanrebackend.dto.review.CreateReviewRequest;
import com.vesanrebackend.dto.review.ReviewView;
import com.vesanrebackend.service.booking.BookingService;
import com.vesanrebackend.service.review.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("hasRole('CUSTOMER')")
public class BookingController {
    private final BookingService bookings;

    private final ReviewService reviews;

    public BookingController(BookingService bookings, ReviewService reviews) {
        this.bookings = bookings;
        this.reviews = reviews;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDetail create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateBookingRequest req) {
        return bookings.create(UUID.fromString(jwt.getSubject()), req);
    }

    @GetMapping
    public PageResponse<BookingSummary> list(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam(required = false) BookingStatus status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return bookings.list(UUID.fromString(jwt.getSubject()), status, page, size);
    }

    @GetMapping("/{id}")
    public BookingDetail get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return bookings.get(UUID.fromString(jwt.getSubject()), id);
    }

    @PostMapping("/{id}/cancel")
    public BookingDetail cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @Valid @RequestBody(required = false) CancelBookingRequest req) {
        return bookings.cancel(UUID.fromString(jwt.getSubject()), id, req == null ? null : req.reason());
    }

    @PostMapping("/{id}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewView review(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                             @Valid @RequestBody CreateReviewRequest req) {
        return reviews.create(UUID.fromString(jwt.getSubject()), id, req);
    }
}
