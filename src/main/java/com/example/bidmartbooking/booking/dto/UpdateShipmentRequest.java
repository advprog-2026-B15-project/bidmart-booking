package com.example.bidmartbooking.booking.dto;

import com.example.bidmartbooking.booking.model.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateShipmentRequest {

    @NotNull(message = "status is required")
    private ShipmentStatus status;

    private String trackingNumber;

    private String courierName;
}
