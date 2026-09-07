package com.vesanrebackend.entity;

import com.vesanrebackend.entity.enums.BookingActorType;
import com.vesanrebackend.entity.enums.BookingStatus;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Lịch sử thay đổi trạng thái đơn đặt sân.
 */
@Entity
@Table(name = "booking_status_history", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class BookingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    // Đơn đặt sân liên quan.
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    // Trạng thái trước thay đổi.
    private BookingStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    // Trạng thái sau thay đổi.
    private BookingStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    // Loại chủ thể gây ra thay đổi.
    private BookingActorType actorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    // Người dùng thực hiện hành động.
    private UserAccount actorUser;

    @Column(name = "reason_code", length = 80)
    // Mã lý do thay đổi trạng thái.
    private String reasonCode;

    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    // Dữ liệu bổ sung, dạng JSON.
    private String metadata = "{}";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;
}
