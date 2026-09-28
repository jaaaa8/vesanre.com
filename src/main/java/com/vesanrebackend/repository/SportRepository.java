package com.vesanrebackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.vesanrebackend.entity.Sport;

public interface SportRepository extends JpaRepository<Sport, Long> {
}
