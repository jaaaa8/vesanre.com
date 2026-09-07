package com.vesanrebackend.entity;

import com.vesanrebackend.entity.enums.PaymentIntentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ý định thanh toán cho đơn đặt sân.
 */
@Entity
@Table(name = "payment_intents", schema = "sporthub", uniqueConstraints = {
        @UniqueConstraint(name = "uq_payment_intents_idempotency", columnNames = {"provider_code", "idempotency_key"})
})
@Getter
@Setter
@NoArgsConstructor
public class PaymentIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    // Đơn đặt sân liên quan.
    private Booking booking;

    @Column(name = "provider_code", nullable = false, length = 50)
    // Mã nhà cung cấp thanh toán.
    private String providerCode;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    // Khóa chống xử lý hoàn tiền lặp.
    private String idempotencyKey;

    @Column(name = "provider_intent_id", length = 160)
    // Mã ý định tại nhà cung cấp.
    private String providerIntentId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    // Số tiền.
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    // Mã tiền tệ ISO 4217.
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    // Trạng thái hiện tại.
    private PaymentIntentStatus status = PaymentIntentStatus.CREATED;

    @Column(name = "expires_at", columnDefinition = "timestamp with time zone")
    // Thời điểm hết hạn.
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm cập nhật gần nhất.
    private Instant updatedAt;

    @OneToMany(mappedBy = "paymentIntent")
    // Danh sách giao dịch thanh toán.
    private List<PaymentTransaction> transactions = new ArrayList<>();
}
