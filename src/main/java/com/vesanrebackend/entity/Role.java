package com.vesanrebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Vai trò phân quyền trong hệ thống.
 */
@Entity
@Table(name = "roles", schema = "sporthub")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @Column(name = "code", nullable = false, length = 30)
    // Mã nghiệp vụ duy nhất.
    private String code;

    @Column(name = "name", nullable = false, length = 80)
    // Tên hiển thị.
    private String name;

    @Column(name = "description", columnDefinition = "text")
    // Mô tả chi tiết.
    private String description;

    @OneToMany(mappedBy = "role")
    // Danh sách vai trò người dùng.
    private List<UserRole> userRoles = new ArrayList<>();
}
