package com.example.bidmartbooking.booking.dto;

import com.example.bidmartbooking.booking.model.BookingStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryConfirmationResponse {
    private Long bookingId;
    private BookingStatus status;
}
