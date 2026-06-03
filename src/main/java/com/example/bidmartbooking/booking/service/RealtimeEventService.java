package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.dto.NotificationResponse;
import com.example.bidmartbooking.booking.dto.BookingStatusUpdateResponse;
import com.example.bidmartbooking.booking.dto.RealtimeAuctionUpdateResponse;
import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingStatus;
import com.example.bidmartbooking.booking.model.Notification;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class RealtimeEventService {

    private static final Long NO_TIMEOUT = 0L;

    private final Map<String, List<SseEmitter>> emittersByUserId =
            new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
        emittersByUserId
                .computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(throwable -> removeEmitter(userId, emitter));
        send(userId, "connected", "connected");
        return emitter;
    }

    @Scheduled(fixedRateString = "${realtime.sse.heartbeat-ms:25000}")
    public void sendHeartbeat() {
        emittersByUserId.forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                } catch (IOException | IllegalStateException exception) {
                    removeEmitter(userId, emitter);
                }
            }
        });
    }

    public void publishNotification(Notification notification) {
        send(
                notification.getUserId(),
                "notification",
                toNotificationResponse(notification)
        );
    }

    public void publishAuctionUpdate(
            String userId,
            RealtimeAuctionUpdateResponse update
    ) {
        send(userId, "auction-update", update);
    }

    public void publishBookingStatusChange(
            Booking booking,
            BookingStatus fromStatus,
            BookingStatus toStatus,
            String changedByUserId,
            String changedByRole,
            String reason
    ) {
        BookingStatusUpdateResponse update = new BookingStatusUpdateResponse();
        update.setBookingId(booking.getId());
        update.setAuctionId(booking.getAuctionId());
        update.setFromStatus(fromStatus);
        update.setToStatus(toStatus);
        update.setChangedByUserId(changedByUserId);
        update.setChangedByRole(changedByRole);
        update.setReason(reason);

        Set<String> recipients = new LinkedHashSet<>();
        recipients.add(booking.getBuyerUserId());
        recipients.add(booking.getSellerUserId());
        recipients.forEach(userId -> send(
                userId,
                "booking-status-changed",
                update
        ));
    }

    int getSubscriberCount(String userId) {
        return emittersByUserId.getOrDefault(userId, List.of()).size();
    }

    private void send(String userId, String eventName, Object data) {
        List<SseEmitter> emitters = emittersByUserId.getOrDefault(userId, List.of());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException exception) {
                removeEmitter(userId, emitter);
            }
        }
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUserId.remove(userId);
        }
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
        }
        return response;
    }
}
