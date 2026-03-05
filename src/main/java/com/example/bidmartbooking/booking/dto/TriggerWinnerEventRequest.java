package com.example.bidmartbooking.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TriggerWinnerEventRequest {

    @NotBlank(message = "auctionId is required")
    private String auctionId;

    @NotBlank(message = "listingId is required")
    private String listingId;

    @NotBlank(message = "sellerUserId is required")
    private String sellerUserId;

    @NotBlank(message = "winnerUserId is required")
    private String winnerUserId;

    @NotNull(message = "finalPrice is required")
    @Min(value = 0, message = "finalPrice must be >= 0")
    private Long finalPrice;

    private String currency;

    @NotBlank(message = "itemName is required")
    private String itemName;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be >= 1")
    private Integer quantity;

    private List<String> loserUserIds;
}
