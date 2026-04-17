package com.example.bidmartbooking.booking.controller;

import com.example.bidmartbooking.booking.dto.MarkNotificationReadRequest;
import com.example.bidmartbooking.booking.dto.NotificationPreferenceResponse;
import com.example.bidmartbooking.booking.dto.NotificationResponse;
import com.example.bidmartbooking.booking.dto.UpdateNotificationPreferenceRequest;
import com.example.bidmartbooking.booking.model.Notification;
import com.example.bidmartbooking.booking.model.NotificationPreference;
import com.example.bidmartbooking.booking.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Notifications", description = "Notification inbox and preference endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    @Operation(summary = "List my notifications")
    public List<NotificationResponse> getMyNotifications(
            @Parameter(description = "Current user id", required = true)
            @RequestHeader("X-User-Id") String userId
    ) {
        return notificationService.getMyNotifications(userId)
                .stream()
                .map(this::toNotificationResponse)
                .toList();
    }

    @GetMapping("/preferences/me")
    @Operation(summary = "Get my notification preferences")
    public NotificationPreferenceResponse getMyNotificationPreference(
            @Parameter(description = "Current user id", required = true)
            @RequestHeader("X-User-Id") String userId
    ) {
        return toNotificationPreferenceResponse(
                notificationService.getMyNotificationPreference(userId)
        );
    }

    @PatchMapping("/preferences/me")
    @Operation(summary = "Update my notification preferences")
    public NotificationPreferenceResponse updateMyNotificationPreference(
            @Parameter(description = "Current user id", required = true)
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UpdateNotificationPreferenceRequest request
    ) {
        NotificationPreference preference = notificationService.upsertNotificationPreference(
                userId,
                request.getEmailEnabled(),
                request.getInAppEnabled()
        );
        return toNotificationPreferenceResponse(preference);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public NotificationResponse markNotificationAsRead(
            @Parameter(description = "Notification id", required = true)
            @PathVariable Long id,
            @Parameter(description = "Current user id", required = true)
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

    private NotificationPreferenceResponse toNotificationPreferenceResponse(
            NotificationPreference preference
    ) {
        NotificationPreferenceResponse response = new NotificationPreferenceResponse();
        response.setUserId(preference.getUserId());
        response.setEmailEnabled(preference.getEmailEnabled());
        response.setInAppEnabled(preference.getInAppEnabled());
        return response;
    }
}
