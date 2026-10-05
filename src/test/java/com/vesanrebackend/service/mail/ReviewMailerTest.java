package com.vesanrebackend.service.mail;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Loại test: unit - ReviewMailer với ApplicationEventPublisher được mock.
 * Thành phần: ReviewMailer.send (email thông báo khi admin duyệt/từ chối provider, venue, change request).
 */
class ReviewMailerTest {
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ReviewMailer mailer = new ReviewMailer(events, null, "no-reply@vesanre.local");

    // Thành phần: ReviewMailer.send
    // Kiểm tra: Người nhận null/rỗng/toàn khoảng trắng thì không publish event.
    @Test
    void nullOrBlankRecipientPublishesNothing() {
        mailer.send(null, "s", "m", null);
        mailer.send("", "s", "m", null);
        mailer.send("  ", "s", "m", "r");
        verifyNoInteractions(events);
    }

    // Thành phần: ReviewMailer.send
    // Kiểm tra: Người nhận hợp lệ thì publish một ReviewMail event.
    @Test
    void realRecipientPublishesEvent() {
        mailer.send("a@example.com", "s", "m", null);
        verify(events).publishEvent(any(ReviewMailer.ReviewMail.class));
    }
}
