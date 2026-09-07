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

/**
 * Đơn đặt sân của khách hàng.
 */
@Entity
@Table(name = "bookings", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @Column(name = "booking_code", nullable = false, length = 30, unique = true)
    // Mã đặt sân dùng để tra cứu.
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    // Khách hàng đặt sân.
    private UserAccount customer;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_block_id", nullable = false, unique = true)
    // Khoảng lịch của đơn đặt sân.
    private CourtScheduleBlock scheduleBlock;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    // Trạng thái hiện tại.
    private BookingStatus status = BookingStatus.HELD;

    @Column(name = "hold_expires_at", columnDefinition = "timestamp with time zone")
    // Thời điểm giữ chỗ hết hạn.
    private Instant holdExpiresAt;

    @Column(name = "shop_name_snapshot", nullable = false, length = 160)
    // Tên cửa hàng được lưu khi đặt.
    private String shopNameSnapshot;

    @Column(name = "venue_name_snapshot", nullable = false, length = 160)
    // Tên địa điểm được lưu khi đặt.
    private String venueNameSnapshot;

    @Column(name = "court_name_snapshot", nullable = false, length = 120)
    // Tên sân được lưu khi đặt.
    private String courtNameSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2)
    // Đơn giá được lưu khi đặt.
    private BigDecimal unitPriceSnapshot;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    // Tổng tiền trước tiền đặt cọc.
    private BigDecimal subtotalAmount;

    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 2)
    // Số tiền đặt cọc.
    private BigDecimal depositAmount;

    @Column(name = "currency", nullable = false, length = 3)
    // Mã tiền tệ ISO 4217.
    private String currency;

    @Column(name = "price_breakdown_snapshot", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    // Bản chụp chi tiết giá khi đặt, dạng JSON.
    private String priceBreakdownSnapshot;

    @Column(name = "cancellation_policy_snapshot", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    // Bản chụp chính sách hủy khi đặt sân, dạng JSON.
    private String cancellationPolicySnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_source", length = 20)
    // Nguồn khởi tạo việc hủy.
    private CancellationSource cancellationSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    // Người dùng thực hiện việc hủy.
    private UserAccount cancelledBy;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    // Lý do hủy đơn.
    private String cancellationReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;

    @Column(name = "confirmed_at", columnDefinition = "timestamp with time zone")
    // Thời điểm đơn được xác nhận.
    private Instant confirmedAt;

    @Column(name = "cancelled_at", columnDefinition = "timestamp with time zone")
    // Thời điểm đơn bị hủy.
    private Instant cancelledAt;

    @Column(name = "completed_at", columnDefinition = "timestamp with time zone")
    // Thời điểm hoàn tất.
    private Instant completedAt;

    @OneToMany(mappedBy = "booking")
    // Lịch sử trạng thái của đơn.
    private List<BookingStatusHistory> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "booking")
    // Danh sách ý định thanh toán.
    private List<PaymentIntent> paymentIntents = new ArrayList<>();

    @OneToMany(mappedBy = "booking")
    // Danh sách yêu cầu hoàn tiền.
    private List<RefundRequest> refundRequests = new ArrayList<>();

    @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY)
    // Đánh giá gắn với đơn đặt sân.
    private Review review;
}
