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

@Entity
@Table(name = "court_schedule_blocks", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class CourtScheduleBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_kind", nullable = false, length = 20)
    private ScheduleBlockKind blockKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private ScheduleBlockState state = ScheduleBlockState.ACTIVE;

    @Column(name = "start_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant startAt;

    @Column(name = "end_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant endAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserAccount createdBy;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "released_at", columnDefinition = "timestamp with time zone")
    private Instant releasedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    private Instant createdAt;

    @OneToOne(mappedBy = "scheduleBlock", fetch = FetchType.LAZY)
    private Booking booking;
}
