package com.example.bidmartbooking.booking.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RealtimeAuctionUpdateResponse {
    private String auctionId;
    private String itemName;
    private Long currentPrice;
    private String eventType;
}
