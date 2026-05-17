package com.example.bidmartbooking.booking.repository;

import com.example.bidmartbooking.booking.model.Dispute;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    Optional<Dispute> findByBookingId(Long bookingId);
    boolean existsByBookingId(Long bookingId);
}
