package com.vesanrebackend.controller.admin;

import com.vesanrebackend.dto.report.FinanceReport;
import com.vesanrebackend.dto.report.TopShop;
import com.vesanrebackend.service.report.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {
    private final ReportService reports;

    public AdminReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/top-shops")
    public List<TopShop> topShops(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                                  @RequestParam(defaultValue = "10") int limit) {
        return reports.topShops(month, limit);
    }

    @GetMapping("/finance")
    public FinanceReport finance(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return reports.finance(month);
    }
}
