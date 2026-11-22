package com.vesanrebackend.service.coin;

import com.vesanrebackend.dto.coin.TopUpView;
import com.vesanrebackend.entity.coin.CoinTopUp;
import com.vesanrebackend.entity.coin.CoinWallet;
import com.vesanrebackend.entity.enums.CoinEntryType;
import com.vesanrebackend.entity.enums.CoinTopUpStatus;
import com.vesanrebackend.entity.enums.WebhookEventStatus;
import com.vesanrebackend.entity.payment.PaymentWebhookEvent;
import com.vesanrebackend.repository.CoinTopUpRepository;
import com.vesanrebackend.repository.PaymentWebhookEventRepository;
import com.vesanrebackend.service.payment.GatewayPayment;
import com.vesanrebackend.service.payment.TopUpGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Coin top-up orders and their paid notifications. */
@Service
public class TopUpService {
    private static final Logger log = LoggerFactory.getLogger(TopUpService.class);
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final long VND_PER_COIN = 1000;

    private final SecureRandom random = new SecureRandom();
    private final CoinTopUpRepository topUps;
    private final PaymentWebhookEventRepository events;
    private final CoinWalletService wallets;
    private final List<TopUpGateway> gateways;
    private final Clock clock;

    public TopUpService(CoinTopUpRepository topUps, PaymentWebhookEventRepository events, CoinWalletService wallets,
                        List<TopUpGateway> gateways, Clock clock) {
        this.topUps = topUps;
        this.events = events;
        this.wallets = wallets;
        this.gateways = gateways;
        this.clock = clock;
    }

    @Transactional
    public TopUpView create(UUID userId, long coins) {
        if (gateways.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Top-up is not available");
        }
        TopUpGateway gateway = gateways.get(0);
        CoinTopUp t = new CoinTopUp();
        t.setWallet(wallets.lockUserWallet(userId));
        t.setTransferCode(newTransferCode());
        t.setCoinAmount(coins);
        t.setAmountVnd(BigDecimal.valueOf(coins * VND_PER_COIN));
        t.setProviderCode(gateway.code());
        t.setExpiresAt(clock.instant().plus(Duration.ofMinutes(15)));
        return view(topUps.saveAndFlush(t));
    }

    @Transactional(readOnly = true)
    public TopUpView get(UUID userId, UUID topUpId) {
        return view(topUps.findByIdAndWalletUserId(topUpId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Top-up not found")));
    }

    /**
     * Verifies, stores and applies a paid notification. The transaction sits on this method itself: the wallet
     * lock must outlive the balance write, and a self-call to a @Transactional helper would bypass the proxy.
     */
    @Transactional
    public void receive(String providerCode, byte[] rawBody, HttpHeaders headers) {
        TopUpGateway gateway = gateways.stream().filter(g -> g.code().equals(providerCode)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown payment provider"));
        if (!gateway.verify(rawBody, headers)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid signature");
        }
        GatewayPayment payment = gateway.parse(rawBody);
        Instant now = clock.instant();

        UUID eventId = UUID.randomUUID();
        if (events.insertIfAbsent(eventId, providerCode, payment.eventId(), "TOP_UP_PAID",
                new String(rawBody, StandardCharsets.UTF_8), now) == 0) {
            return; // duplicate event
        }
        PaymentWebhookEvent event = events.findById(eventId).orElseThrow();

        CoinTopUp topUp = topUps.findByTransferCodeForUpdate(payment.transferCode()).orElse(null);
        String ignored = null;
        if (topUp == null) {
            ignored = "No top-up for transfer code";
        } else if (!topUp.getProviderCode().equals(providerCode)) {
            ignored = "Provider mismatch";
        } else if (payment.amountVnd() != topUp.getCoinAmount() * VND_PER_COIN) {
            ignored = "Amount mismatch";
        } else if (topUp.getStatus() == CoinTopUpStatus.PAID) {
            ignored = "Top-up already paid";
        }
        event.setProcessedAt(now);
        if (ignored != null) {
            event.setStatus(WebhookEventStatus.IGNORED);
            event.setErrorMessage(ignored);
            log.warn("Webhook {} {} ignored: {}", providerCode, payment.eventId(), ignored);
            return;
        }
        topUp.setStatus(CoinTopUpStatus.PAID);
        topUp.setPaidAt(now);
        topUp.setProviderTransactionId(payment.transactionId());
        topUp.setWebhookEvent(event);
        // Before apply(): its flush must not see processed_at with status RECEIVED (check constraint).
        event.setStatus(WebhookEventStatus.PROCESSED);
        // Lock by id: reading anything off the lazy wallet first would load it unlocked with a stale balance.
        CoinWallet wallet = wallets.lockWallet(topUp.getWallet().getId());
        wallets.apply(wallet, CoinEntryType.TOP_UP, topUp.getCoinAmount(), null, topUp);
    }

    private TopUpView view(CoinTopUp t) {
        String url = gateways.stream().filter(g -> g.code().equals(t.getProviderCode())).findFirst()
                .map(g -> g.paymentUrl(t)).orElse(null);
        return new TopUpView(t.getId(), t.getTransferCode(), t.getCoinAmount(), t.getAmountVnd().setScale(0),
                t.getStatus().name(), t.getExpiresAt(), t.getPaidAt(), url);
    }

    private String newTransferCode() {
        StringBuilder sb = new StringBuilder("VSR");
        for (int i = 0; i < 8; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
