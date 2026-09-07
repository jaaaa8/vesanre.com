package com.vesanrebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Môn thể thao được hệ thống hỗ trợ.
 */
@Entity
@Table(name = "sports", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class Sport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    // Mã định danh bản ghi.
    private UUID id;

    @Column(name = "code", nullable = false, length = 50, unique = true)
    // Mã nghiệp vụ duy nhất.
    private String code;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    // Tên hiển thị.
    private String name;

    @Column(name = "is_active", nullable = false)
    // Cho biết bản ghi đang hoạt động.
    private Boolean isActive = Boolean.TRUE;

    @OneToMany(mappedBy = "sport")
    // Danh sách sân liên quan.
    private List<CourtSport> courts = new ArrayList<>();
}
