package com.example.bidmartbooking.booking.repository;

import com.example.bidmartbooking.booking.model.BookingStatusAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingStatusAuditLogRepository
        extends JpaRepository<BookingStatusAuditLog, Long> {

    List<BookingStatusAuditLog> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
