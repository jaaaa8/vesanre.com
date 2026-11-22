package com.vesanrebackend.controller.provider;

import com.vesanrebackend.dto.report.MonthlyBookings;
import com.vesanrebackend.service.report.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/provider/reports")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderReportController {
    private final ReportService reports;

    public ProviderReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/monthly-bookings")
    public List<MonthlyBookings> monthlyBookings(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestParam(defaultValue = "6") int months) {
        return reports.providerMonthly(UUID.fromString(jwt.getSubject()), months);
    }
}
