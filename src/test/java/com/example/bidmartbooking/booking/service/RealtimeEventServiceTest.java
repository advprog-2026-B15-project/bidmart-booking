package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.dto.RealtimeAuctionUpdateResponse;
import com.example.bidmartbooking.booking.model.Notification;
import com.example.bidmartbooking.booking.model.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RealtimeEventServiceTest {

    @Test
    void shouldRegisterSubscriber() {
        RealtimeEventService service = new RealtimeEventService();

        SseEmitter emitter = service.subscribe("user-1");

        assertNotNull(emitter);
        assertEquals(1, service.getSubscriberCount("user-1"));
    }

    @Test
    void shouldIgnoreNotificationWhenUserHasNoSubscriber() {
        RealtimeEventService service = new RealtimeEventService();
        Notification notification = new Notification();
        notification.setUserId("user-2");
        notification.setType(NotificationType.WIN);
        notification.setTitle("Win");
        notification.setMessage("Message");

        service.publishNotification(notification);

        assertEquals(0, service.getSubscriberCount("user-2"));
    }

    @Test
    void shouldIgnoreAuctionUpdateWhenUserHasNoSubscriber() {
        RealtimeEventService service = new RealtimeEventService();
        RealtimeAuctionUpdateResponse update = new RealtimeAuctionUpdateResponse();
        update.setAuctionId("auc-1");
        update.setItemName("Keyboard");
        update.setCurrentPrice(1000L);
        update.setEventType("BidPlaced");

        service.publishAuctionUpdate("user-3", update);

        assertEquals(0, service.getSubscriberCount("user-3"));
    }
}
