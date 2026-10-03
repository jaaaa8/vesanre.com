package com.vesanrebackend.repository;

import com.vesanrebackend.entity.account.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.UserRoleId> {
}
