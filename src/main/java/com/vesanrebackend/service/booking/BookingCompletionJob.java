package com.vesanrebackend.service.booking;

import com.vesanrebackend.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.UUID;

/** Completes ended bookings. Not transactional: each complete() runs in its own transaction. */
@Component
public class BookingCompletionJob {
    private static final Logger log = LoggerFactory.getLogger(BookingCompletionJob.class);

    private final BookingRepository bookings;
    private final BookingLifecycle lifecycle;
    private final Clock clock;

    public BookingCompletionJob(BookingRepository bookings, BookingLifecycle lifecycle, Clock clock) {
        this.bookings = bookings;
        this.lifecycle = lifecycle;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.jobs.completion-delay-ms:300000}")
    public void run() {
        // ponytail: 100 bookings per run; loop until empty if backlog grows
        for (UUID id : bookings.findIdsToComplete(clock.instant(), PageRequest.of(0, 100))) {
            try {
                lifecycle.complete(id);
            } catch (ResponseStatusException e) {
                log.info("Booking {} not completed: {}", id, e.getReason());
            } catch (RuntimeException e) {
                log.error("Booking {} completion failed", id, e);
            }
        }
    }
}
