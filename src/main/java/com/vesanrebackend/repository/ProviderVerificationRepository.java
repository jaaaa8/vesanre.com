package com.vesanrebackend.repository;

import com.vesanrebackend.entity.ProviderVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProviderVerificationRepository extends JpaRepository<ProviderVerification, UUID> {
}
