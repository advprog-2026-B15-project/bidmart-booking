package com.example.bidmartbooking.booking.repository;

import com.example.bidmartbooking.booking.model.DeadLetterEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, Long> {
}
