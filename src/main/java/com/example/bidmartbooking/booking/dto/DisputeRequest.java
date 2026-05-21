package com.example.bidmartbooking.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DisputeRequest {

    @NotBlank(message = "reason is required")
    @Size(min = 10, max = 1000, message = "reason must be between 10 and 1000 characters")
    private String reason;
}
