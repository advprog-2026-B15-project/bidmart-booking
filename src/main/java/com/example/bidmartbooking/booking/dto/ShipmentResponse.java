package com.example.bidmartbooking.booking.dto;

import com.example.bidmartbooking.booking.model.ShipmentStatus;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipmentResponse {
    private ShipmentStatus status;
    private String trackingNumber;
    private String courierName;
    private OffsetDateTime shippedAt;
    private OffsetDateTime deliveredAt;
}
