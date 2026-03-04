package com.example.bidmartbooking.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkNotificationReadRequest {

    @NotNull(message = "read is required")
    private Boolean read;
}
