package com.vesanrebackend.controller.provider;

import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.booking.BookingDetail;
import com.vesanrebackend.dto.booking.BookingSummary;
import com.vesanrebackend.dto.booking.ProviderCancelRequest;
import com.vesanrebackend.dto.coin.WalletView;
import com.vesanrebackend.entity.enums.BookingStatus;
import com.vesanrebackend.service.booking.ProviderBookingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/provider")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderBookingController {
    private final ProviderBookingService bookings;

    public ProviderBookingController(ProviderBookingService bookings) {
        this.bookings = bookings;
    }

    @GetMapping("/bookings")
    public PageResponse<BookingSummary> list(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam(required = false) UUID venueId,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                             @RequestParam(required = false) BookingStatus status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return bookings.list(UUID.fromString(jwt.getSubject()), venueId, date, status, page, size);
    }

    @GetMapping("/bookings/{id}")
    public BookingDetail get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return bookings.get(UUID.fromString(jwt.getSubject()), id);
    }

    @PostMapping("/bookings/{id}/cancel")
    public BookingDetail cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @Valid @RequestBody ProviderCancelRequest req) {
        return bookings.cancel(UUID.fromString(jwt.getSubject()), id, req.reason());
    }

    @GetMapping("/wallet")
    public WalletView wallet(@AuthenticationPrincipal Jwt jwt) {
        return bookings.wallet(UUID.fromString(jwt.getSubject()));
    }
}
