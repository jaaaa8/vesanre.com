package com.vesanrebackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.vesanrebackend.entity.catalog.Sport;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SportRepository extends JpaRepository<Sport, UUID> {
    List<Sport> findByIsActiveTrueOrderByName();

    Optional<Sport> findByCodeAndIsActiveTrue(String code);
}
