package com.example.bidmartbooking.booking.dto;

import com.example.bidmartbooking.booking.model.ShipmentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipmentUpdateResponse {
    private Long bookingId;
    private ShipmentStatus shipmentStatus;
    private String trackingNumber;
    private String courierName;
}
