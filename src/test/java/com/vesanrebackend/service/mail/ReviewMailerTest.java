package com.vesanrebackend.service.mail;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ReviewMailerTest {
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ReviewMailer mailer = new ReviewMailer(events, null, "no-reply@vesanre.local");

    @Test
    void nullOrBlankRecipientPublishesNothing() {
        mailer.send(null, "s", "m", null);
        mailer.send("", "s", "m", null);
        mailer.send("  ", "s", "m", "r");
        verifyNoInteractions(events);
    }

    @Test
    void realRecipientPublishesEvent() {
        mailer.send("a@example.com", "s", "m", null);
        verify(events).publishEvent(any(ReviewMailer.ReviewMail.class));
    }
}
