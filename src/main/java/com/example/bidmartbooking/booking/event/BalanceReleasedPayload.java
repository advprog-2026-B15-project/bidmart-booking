package com.example.bidmartbooking.booking.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BalanceReleasedPayload {
    private String bookingId;
    private String auctionId;
    private String userId;
    private Long amount;
    private String currency;
    private String releaseReference;
}
