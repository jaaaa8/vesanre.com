package com.vesanrebackend.service.coin;

import com.vesanrebackend.repository.CoinTopUpRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Marks unpaid top-ups past their deadline as EXPIRED; a late payment still credits them. */
@Component
public class TopUpExpiryJob {
    private final CoinTopUpRepository topUps;
    private final Clock clock;

    public TopUpExpiryJob(CoinTopUpRepository topUps, Clock clock) {
        this.topUps = topUps;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.jobs.top-up-expiry-delay-ms:60000}")
    @Transactional
    public void run() {
        topUps.expirePending(clock.instant());
    }
}
