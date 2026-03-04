package com.example.bidmartbooking.booking.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuctionClosedPayload {
    private String auctionId;
    private String listingId;
    private String closedAt;
    private Boolean hasWinner;
    private String winnerUserId;
}
