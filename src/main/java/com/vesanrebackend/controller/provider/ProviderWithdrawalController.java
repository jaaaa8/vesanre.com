package com.vesanrebackend.controller.provider;

import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.coin.CreateWithdrawalRequest;
import com.vesanrebackend.dto.coin.WithdrawalView;
import com.vesanrebackend.service.coin.WithdrawalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/provider/withdrawals")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderWithdrawalController {
    private final WithdrawalService withdrawals;

    public ProviderWithdrawalController(WithdrawalService withdrawals) {
        this.withdrawals = withdrawals;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WithdrawalView create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateWithdrawalRequest req) {
        return withdrawals.create(UUID.fromString(jwt.getSubject()), req);
    }

    @GetMapping
    public PageResponse<WithdrawalView> list(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return withdrawals.list(UUID.fromString(jwt.getSubject()), page, size);
    }
}
