package com.vesanrebackend.service.notification;

import com.vesanrebackend.entity.OutboxEvent;
import com.vesanrebackend.entity.enums.OutboxStatus;
import com.vesanrebackend.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class OutboxProcessor {
    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);
    private static final int MAX_ATTEMPTS = 5;

    private final OutboxEventRepository outbox;
    private final OutboxHandler handler;
    private final Clock clock;

    public OutboxProcessor(OutboxEventRepository outbox, OutboxHandler handler, Clock clock) {
        this.outbox = outbox;
        this.handler = handler;
        this.clock = clock;
    }

    /** Claims and handles one event; false when nothing is due. Backoff 1, 2, 4, 8 minutes, then FAILED. */
    @Transactional
    public boolean processOne() {
        Instant now = clock.instant();
        OutboxEvent e = outbox.claimNext(now).orElse(null);
        if (e == null) {
            return false;
        }
        try {
            handler.handle(e);
            e.setStatus(OutboxStatus.PUBLISHED);
            e.setPublishedAt(now);
        } catch (RuntimeException ex) {
            int attempts = e.getAttemptCount() + 1;
            e.setAttemptCount(attempts);
            if (attempts >= MAX_ATTEMPTS) {
                e.setStatus(OutboxStatus.FAILED);
                log.error("Outbox event {} failed permanently", e.getEventKey(), ex);
            } else {
                e.setStatus(OutboxStatus.RETRY_PENDING);
                e.setNextAttemptAt(now.plus(1L << (attempts - 1), ChronoUnit.MINUTES));
                log.warn("Outbox event {} failed (attempt {})", e.getEventKey(), attempts, ex);
            }
        }
        return true;
    }
}
