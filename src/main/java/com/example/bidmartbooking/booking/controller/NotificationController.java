package com.example.bidmartbooking.booking.controller;

import com.example.bidmartbooking.booking.dto.MarkNotificationReadRequest;
import com.example.bidmartbooking.booking.dto.NotificationResponse;
import com.example.bidmartbooking.booking.model.Notification;
import com.example.bidmartbooking.booking.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    public List<NotificationResponse> getMyNotifications(
            @RequestHeader("X-User-Id") String userId
    ) {
        return notificationService.getMyNotifications(userId)
                .stream()
                .map(this::toNotificationResponse)
                .toList();
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markNotificationAsRead(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody MarkNotificationReadRequest request
    ) {
        if (!Boolean.TRUE.equals(request.getRead())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "read must be true"
            );
        }

        Notification notification = notificationService.markNotificationAsRead(id, userId);
        return toNotificationResponse(notification);
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setIsRead(notification.getIsRead());
        response.setReadAt(notification.getReadAt());
        response.setCreatedAt(notification.getCreatedAt());
        response.setRelatedAuctionId(notification.getRelatedAuctionId());
        if (notification.getRelatedBooking() != null) {
            response.setRelatedBookingId(notification.getRelatedBooking().getId());
        } else {
            response.setRelatedBookingId(null);
        }
        return response;
    }
}
