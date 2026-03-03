package com.example.bidmartbooking.booking.repository;

import com.example.bidmartbooking.booking.model.Shipment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByBookingId(Long bookingId);
}
