package com.vesanrebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Thông báo gửi đến người dùng.
 */
@Entity
@Table(name = "notifications", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    // Người dùng liên quan.
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    // Đơn đặt sân liên quan.
    private Booking booking;

    @Column(name = "notification_type", nullable = false, length = 80)
    // Loại thông báo.
    private String notificationType;

    @Column(name = "title", nullable = false, length = 160)
    // Tiêu đề thông báo.
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "text")
    // Nội dung thông báo.
    private String body;

    @Column(name = "dedupe_key", nullable = false, length = 160, unique = true)
    // Khóa chống tạo thông báo trùng.
    private String dedupeKey;

    @Column(name = "read_at", columnDefinition = "timestamp with time zone")
    // Thời điểm đọc thông báo.
    private Instant readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;
}
