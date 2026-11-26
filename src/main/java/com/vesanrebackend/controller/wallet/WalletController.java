package com.vesanrebackend.controller.wallet;

import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.coin.CoinEntryItem;
import com.vesanrebackend.dto.coin.CreateTopUpRequest;
import com.vesanrebackend.dto.coin.TopUpView;
import com.vesanrebackend.dto.coin.WalletView;
import com.vesanrebackend.entity.coin.CoinTopUp;
import com.vesanrebackend.entity.enums.CoinTopUpStatus;
import com.vesanrebackend.repository.CoinTopUpRepository;
import com.vesanrebackend.service.coin.CoinWalletService;
import com.vesanrebackend.service.payment.FakeTopUpGateway;
import com.vesanrebackend.service.coin.TopUpService;
import com.vesanrebackend.util.Paging;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    private final CoinWalletService wallets;
    private final TopUpService topUps;

    private final CoinTopUpRepository topUpRepo;
    private final ObjectProvider<FakeTopUpGateway> fakeGateway;
    private final Clock clock;

    public WalletController(CoinWalletService wallets, TopUpService topUps, CoinTopUpRepository topUpRepo,
                            ObjectProvider<FakeTopUpGateway> fakeGateway, Clock clock) {
        this.wallets = wallets;
        this.topUps = topUps;
        this.topUpRepo = topUpRepo;
        this.fakeGateway = fakeGateway;
        this.clock = clock;
    }

    @GetMapping
    public WalletView wallet(@AuthenticationPrincipal Jwt jwt) {
        return new WalletView(wallets.userBalance(UUID.fromString(jwt.getSubject())));
    }

    @GetMapping("/entries")
    public PageResponse<CoinEntryItem> entries(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(wallets.userEntries(UUID.fromString(jwt.getSubject()),
                Paging.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))));
    }

    @PostMapping("/top-ups")
    @ResponseStatus(HttpStatus.CREATED)
    public TopUpView createTopUp(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateTopUpRequest request) {
        return topUps.create(UUID.fromString(jwt.getSubject()), request.coins());
    }

    /** Dev-only "I paid" button: runs the same signed path as a real webhook. */
    @PostMapping("/top-ups/{id}/simulate-paid")
    public TopUpView simulatePaid(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        FakeTopUpGateway gateway = fakeGateway.getIfAvailable();
        UUID userId = UUID.fromString(jwt.getSubject());
        CoinTopUp t = topUpRepo.findByIdAndWalletUserId(id, userId).orElse(null);
        if (gateway == null || t == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, gateway == null ? "Not found" : "Top-up not found");
        }
        if (t.getStatus() != CoinTopUpStatus.PAID) {
            if (!clock.instant().isBefore(t.getExpiresAt())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Top-up expired");
            }
            FakeTopUpGateway.SignedEvent e = gateway.signedPaidEvent(t);
            topUps.receive(gateway.code(), e.body().getBytes(StandardCharsets.UTF_8), e.headers());
        }
        return topUps.get(userId, id);
    }

    @GetMapping("/top-ups/{id}")
    public TopUpView topUp(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return topUps.get(UUID.fromString(jwt.getSubject()), id);
    }
}
