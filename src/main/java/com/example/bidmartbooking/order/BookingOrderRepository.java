package com.example.bidmartbooking.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingOrderRepository extends JpaRepository<BookingOrder, Long> {
}
