package com.vesanrebackend.entity;

import com.vesanrebackend.entity.enums.BookingStatus;
import com.vesanrebackend.entity.enums.CancellationSource;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "booking_code", nullable = false, length = 30, unique = true)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserAccount customer;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_block_id", nullable = false, unique = true)
    private CourtScheduleBlock scheduleBlock;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status = BookingStatus.HELD;

    @Column(name = "hold_expires_at", columnDefinition = "timestamp with time zone")
    private Instant holdExpiresAt;

    @Column(name = "shop_name_snapshot", nullable = false, length = 160)
    private String shopNameSnapshot;

    @Column(name = "venue_name_snapshot", nullable = false, length = 160)
    private String venueNameSnapshot;

    @Column(name = "court_name_snapshot", nullable = false, length = 120)
    private String courtNameSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "price_breakdown_snapshot", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String priceBreakdownSnapshot;

    @Column(name = "cancellation_policy_snapshot", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String cancellationPolicySnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_source", length = 20)
    private CancellationSource cancellationSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private UserAccount cancelledBy;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    private Instant createdAt;

    @Column(name = "confirmed_at", columnDefinition = "timestamp with time zone")
    private Instant confirmedAt;

    @Column(name = "cancelled_at", columnDefinition = "timestamp with time zone")
    private Instant cancelledAt;

    @Column(name = "completed_at", columnDefinition = "timestamp with time zone")
    private Instant completedAt;

    @OneToMany(mappedBy = "booking")
    private List<BookingStatusHistory> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "booking")
    private List<PaymentIntent> paymentIntents = new ArrayList<>();

    @OneToMany(mappedBy = "booking")
    private List<RefundRequest> refundRequests = new ArrayList<>();

    @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY)
    private Review review;
}
