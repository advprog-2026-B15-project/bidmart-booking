package com.example.bidmartbooking.booking.repository;

import com.example.bidmartbooking.booking.model.BookingItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {
    List<BookingItem> findByBookingId(Long bookingId);
}
