package com.vesanrebackend.service.notification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxWorker {
    private final OutboxProcessor processor;

    public OutboxWorker(OutboxProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${app.jobs.outbox-delay-ms:10000}")
    public void run() {
        // ponytail: 100 events per tick; raise or loop if the backlog outgrows it
        int handled = 0;
        while (handled < 100 && processor.processOne()) {
            handled++;
        }
    }
}
