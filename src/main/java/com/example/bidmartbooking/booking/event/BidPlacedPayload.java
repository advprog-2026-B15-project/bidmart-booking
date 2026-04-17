package com.example.bidmartbooking.booking.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BidPlacedPayload {
    private String auctionId;
    private String listingId;
    private String sellerUserId;
    private String bidderUserId;
    private String previousHighestBidderUserId;
    private Long bidAmount;
    private String currency;
    private String itemName;
}
