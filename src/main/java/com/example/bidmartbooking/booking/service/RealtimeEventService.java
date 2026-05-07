package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.dto.NotificationResponse;
import com.example.bidmartbooking.booking.dto.RealtimeAuctionUpdateResponse;
import com.example.bidmartbooking.booking.model.Notification;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
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
