package com.example.bidmartbooking.booking.event;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WinnerDeterminedPayload {
    private String auctionId;
    private String listingId;
    private String sellerUserId;
    private String winnerUserId;
    private Long finalPrice;
    private String currency;
    private String itemName;
    private Integer quantity;
    private List<String> loserUserIds;
}
