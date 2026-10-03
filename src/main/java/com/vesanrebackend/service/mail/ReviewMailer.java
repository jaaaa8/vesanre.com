package com.vesanrebackend.service.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Review decision mails: published inside the deciding transaction, delivered only after it commits.
@Component
public class ReviewMailer {
    private static final Logger log = LoggerFactory.getLogger(ReviewMailer.class);

    public record ReviewMail(String to, String subject, String body) {
    }

    private final ApplicationEventPublisher events;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public ReviewMailer(ApplicationEventPublisher events, ObjectProvider<JavaMailSender> mailSender,
                        @Value("${app.mail.from}") String from) {
        this.events = events;
        this.mailSender = mailSender;
        this.from = from;
    }

    public void send(String to, String subject, String message, String reason) {
        String body = message + (reason == null ? "" : "\n\nLý do: " + reason) + "\n\nVesanre";
        events.publishEvent(new ReviewMail(to, subject, body));
    }

    // Best effort (spec A3): no retry, no outbox. No JavaMailSender bean (no SPRING_MAIL_HOST) -> log only.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(ReviewMail mail) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("Mail (SMTP not configured) to={} subject={}\n{}", mail.to(), mail.subject(), mail.body());
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(mail.to());
            message.setSubject(mail.subject());
            message.setText(mail.body());
            sender.send(message);
        } catch (RuntimeException e) {
            log.warn("Review mail to {} failed", mail.to(), e);
        }
    }
}
