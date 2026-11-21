package com.vesanrebackend.controller.admin;

import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.admin.ReasonRequest;
import com.vesanrebackend.dto.coin.MarkPaidRequest;
import com.vesanrebackend.dto.coin.WithdrawalView;
import com.vesanrebackend.entity.enums.WithdrawalStatus;
import com.vesanrebackend.service.coin.WithdrawalService;
import jakarta.validation.Valid;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/withdrawals")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWithdrawalController {
    private final WithdrawalService withdrawals;

    public AdminWithdrawalController(WithdrawalService withdrawals) {
        this.withdrawals = withdrawals;
    }

    @GetMapping
    public PageResponse<WithdrawalView> list(@RequestParam(required = false) WithdrawalStatus status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return withdrawals.adminList(status, page, size);
    }

    @PostMapping("/{id}/mark-paid")
    public WithdrawalView markPaid(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                   @Valid @RequestBody MarkPaidRequest req) {
        return withdrawals.markPaid(UUID.fromString(jwt.getSubject()), id, req.bankTransactionRef());
    }

    @PostMapping("/{id}/reject")
    public WithdrawalView reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @Valid @RequestBody ReasonRequest req) {
        return withdrawals.reject(UUID.fromString(jwt.getSubject()), id, req.reason());
    }
}
