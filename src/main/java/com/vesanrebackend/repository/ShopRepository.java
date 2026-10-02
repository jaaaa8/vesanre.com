package com.vesanrebackend.repository;

import com.vesanrebackend.entity.shop.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShopRepository extends JpaRepository<Shop, UUID> {
}
