package com.vesanrebackend.service.mail;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Loại test: Spring integration (@SpringBootTest, TransactionTemplate thật, JavaMailSender mock) - không HTTP, không SMTP thật.
 * Thành phần: ReviewMailer (gửi mail thông báo duyệt/từ chối sau commit).
 */
@SpringBootTest
class ReviewMailerIntegrationTest {
    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private ReviewMailer mailer;

    @Autowired
    private TransactionTemplate tx;

    // Thành phần: ReviewMailer.send
    // Kiểm tra: Sau khi transaction commit mới gọi JavaMailSender.send (bất đồng bộ) với from/to/subject/body và "Lý do: ..." đúng.
    @Test
    void sendsAfterCommitWithReason() {
        tx.executeWithoutResult(status -> mailer.send("owner@example.com", "[Vesanre] Tiêu đề", "Nội dung", "Thiếu ảnh"));
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(2000)).send(captor.capture());
        SimpleMailMessage mail = captor.getValue();
        assertThat(mail.getFrom()).isEqualTo("no-reply@vesanre.local");
        assertThat(mail.getTo()).containsExactly("owner@example.com");
        assertThat(mail.getSubject()).isEqualTo("[Vesanre] Tiêu đề");
        assertThat(mail.getText()).contains("Nội dung").contains("Lý do: Thiếu ảnh");
    }

    // Thành phần: ReviewMailer.send
    // Kiểm tra: Transaction rollback thì không gửi mail (quan sát 500ms).
    @Test
    void rolledBackTransactionSendsNothing() {
        tx.executeWithoutResult(status -> {
            mailer.send("owner@example.com", "[Vesanre] Tiêu đề", "Nội dung", null);
            status.setRollbackOnly();
        });
        verify(mailSender, after(500).never()).send(any(SimpleMailMessage.class));
    }
}
