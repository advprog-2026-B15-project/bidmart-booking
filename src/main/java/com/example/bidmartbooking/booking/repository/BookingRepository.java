package com.example.bidmartbooking.booking.repository;

import com.example.bidmartbooking.booking.model.Booking;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByBuyerUserIdOrderByCreatedAtDesc(String buyerUserId);
    Optional<Booking> findByIdAndBuyerUserId(Long id, String buyerUserId);
    boolean existsBySourceEventId(String sourceEventId);
    boolean existsByAuctionId(String auctionId);
}
