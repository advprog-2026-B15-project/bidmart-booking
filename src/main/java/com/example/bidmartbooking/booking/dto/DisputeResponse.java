package com.example.bidmartbooking.booking.dto;

import com.example.bidmartbooking.booking.model.DisputeStatus;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DisputeResponse {

    private Long id;
    private Long bookingId;
    private String filedByUserId;
    private String reason;
    private DisputeStatus status;
    private String resolutionNote;
    private OffsetDateTime resolvedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
