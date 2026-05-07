package com.example.bidmartbooking.booking.dto;

import com.example.bidmartbooking.booking.model.BookingStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingStatusUpdateResponse {
    private Long bookingId;
    private String auctionId;
    private BookingStatus fromStatus;
    private BookingStatus toStatus;
    private String changedByUserId;
    private String changedByRole;
    private String reason;
}
