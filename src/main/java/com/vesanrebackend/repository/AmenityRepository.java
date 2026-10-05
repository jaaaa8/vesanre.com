package com.vesanrebackend.repository;

import com.vesanrebackend.entity.catalog.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AmenityRepository extends JpaRepository<Amenity, UUID> {
    List<Amenity> findByIsActiveTrueOrderByName();
}
