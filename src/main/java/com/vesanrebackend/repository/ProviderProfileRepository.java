package com.vesanrebackend.repository;

import com.vesanrebackend.entity.ProviderProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, UUID> {
}
