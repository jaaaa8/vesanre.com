package com.vesanrebackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.vesanrebackend.entity.catalog.Sport;

import java.util.UUID;

public interface SportRepository extends JpaRepository<Sport, UUID> {
}
