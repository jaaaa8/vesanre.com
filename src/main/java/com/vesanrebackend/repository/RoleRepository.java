package com.vesanrebackend.repository;

import com.vesanrebackend.entity.account.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {
}
