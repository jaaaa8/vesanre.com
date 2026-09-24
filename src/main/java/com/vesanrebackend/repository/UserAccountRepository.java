package com.vesanrebackend.repository;

import com.vesanrebackend.entity.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<UserAccount> findByEmailNormalized(String emailNormalized);

    boolean existsByEmailNormalized(String emailNormalized);

    boolean existsByPhoneAndIdNot(String phone, UUID id);
}
