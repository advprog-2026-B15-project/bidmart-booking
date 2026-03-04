package com.example.bidmartbooking.booking.dto;

import com.example.bidmartbooking.booking.model.NotificationType;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private Boolean isRead;
    private OffsetDateTime createdAt;
    private OffsetDateTime readAt;
    private String relatedAuctionId;
    private Long relatedBookingId;
}
