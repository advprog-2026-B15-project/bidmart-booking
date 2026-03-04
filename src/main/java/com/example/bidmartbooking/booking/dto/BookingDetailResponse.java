package com.example.bidmartbooking.booking.dto;

import com.example.bidmartbooking.booking.model.BookingStatus;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingDetailResponse {
    private Long id;
    private String auctionId;
    private String listingId;
    private String buyerUserId;
    private String sellerUserId;
    private BookingStatus status;
    private Long totalAmount;
    private String currency;
    private List<BookingItemResponse> items;
    private ShipmentResponse shipment;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
