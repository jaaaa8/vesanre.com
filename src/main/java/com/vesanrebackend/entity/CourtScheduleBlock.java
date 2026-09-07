package com.vesanrebackend.entity;

import com.vesanrebackend.entity.enums.ScheduleBlockKind;
import com.vesanrebackend.entity.enums.ScheduleBlockState;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Khoảng thời gian chiếm dụng hoặc khóa lịch sân.
 */
@Entity
@Table(name = "court_schedule_blocks", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class CourtScheduleBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    // Sân liên quan.
    private Court court;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_kind", nullable = false, length = 20)
    // Loại khoảng khóa lịch.
    private ScheduleBlockKind blockKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    // Trạng thái hiệu lực khoảng lịch.
    private ScheduleBlockState state = ScheduleBlockState.ACTIVE;

    @Column(name = "start_at", nullable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm bắt đầu khoảng lịch.
    private Instant startAt;

    @Column(name = "end_at", nullable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm kết thúc khoảng lịch.
    private Instant endAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    // Người dùng tạo bản ghi.
    private UserAccount createdBy;

    @Column(name = "reason", columnDefinition = "text")
    // Lý do thực hiện.
    private String reason;

    @Column(name = "released_at", columnDefinition = "timestamp with time zone")
    // Thời điểm giải phóng khoảng lịch.
    private Instant releasedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    // Thời điểm tạo bản ghi.
    private Instant createdAt;

    @OneToOne(mappedBy = "scheduleBlock", fetch = FetchType.LAZY)
    // Đơn đặt sân liên quan.
    private Booking booking;
}
