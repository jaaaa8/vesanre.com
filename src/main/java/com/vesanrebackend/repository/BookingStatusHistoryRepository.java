package com.vesanrebackend.repository;

import com.vesanrebackend.entity.booking.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {
}
