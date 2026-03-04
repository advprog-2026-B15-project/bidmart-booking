package com.example.bidmartbooking.booking.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingItemResponse {
    private String listingId;
    private String itemName;
    private Integer quantity;
    private Long unitPrice;
    private Long subtotalAmount;
}
