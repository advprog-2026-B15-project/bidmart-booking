package com.example.bidmartbooking.booking.controller;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.Notification;
import com.example.bidmartbooking.booking.model.NotificationPreference;
import com.example.bidmartbooking.booking.model.NotificationType;
import com.example.bidmartbooking.booking.service.NotificationService;
import com.example.bidmartbooking.booking.service.RealtimeEventService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private RealtimeEventService realtimeEventService;

    @Test
    void shouldGetMyNotifications() throws Exception {
        Notification notification1 = new Notification();
        notification1.setId(1L);
        notification1.setType(NotificationType.WIN);
        notification1.setTitle("Win");
        notification1.setMessage("You won");
        notification1.setIsRead(false);
        notification1.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        notification1.setRelatedAuctionId("auc-1");

        Booking booking = new Booking();
        booking.setId(7L);
        Notification notification2 = new Notification();
        notification2.setId(2L);
        notification2.setType(NotificationType.LOSE);
        notification2.setTitle("Lose");
        notification2.setMessage("You lost");
        notification2.setIsRead(true);
        notification2.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        notification2.setRelatedBooking(booking);

        when(notificationService.getMyNotifications("usr-1"))
                .thenReturn(List.of(notification1, notification2));

        mockMvc.perform(get("/api/notifications/me").header("X-User-Id", "usr-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("WIN"))
                .andExpect(jsonPath("$[1].relatedBookingId").value(7));
    }

    @Test
    void shouldRejectPatchWhenReadFalse() throws Exception {
        mockMvc.perform(patch("/api/notifications/1/read")
                        .header("X-User-Id", "usr-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"read\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldMarkNotificationAsRead() throws Exception {
        Notification notification = new Notification();
        notification.setId(3L);
        notification.setType(NotificationType.WIN);
        notification.setTitle("Win");
        notification.setMessage("Message");
        notification.setIsRead(true);
        notification.setReadAt(OffsetDateTime.now(ZoneOffset.UTC));
        notification.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        when(notificationService.markNotificationAsRead(3L, "usr-1")).thenReturn(notification);

        mockMvc.perform(patch("/api/notifications/3/read")
                        .header("X-User-Id", "usr-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"read\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    void shouldMarkNotificationAsReadWithoutRequestBody() throws Exception {
        Notification notification = new Notification();
        notification.setId(4L);
        notification.setType(NotificationType.INFO);
        notification.setTitle("Info");
        notification.setMessage("Message");
        notification.setIsRead(true);
        notification.setReadAt(OffsetDateTime.now(ZoneOffset.UTC));
        notification.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        when(notificationService.markNotificationAsRead(4L, "usr-1")).thenReturn(notification);

        mockMvc.perform(patch("/api/notifications/4/read")
                        .header("X-User-Id", "usr-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    void shouldGetMyNotificationPreference() throws Exception {
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId("usr-1");
        preference.setEmailEnabled(true);
        preference.setInAppEnabled(false);

        when(notificationService.getMyNotificationPreference("usr-1"))
                .thenReturn(preference);

        mockMvc.perform(get("/api/notifications/preferences/me").header("X-User-Id", "usr-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("usr-1"))
                .andExpect(jsonPath("$.emailEnabled").value(true))
                .andExpect(jsonPath("$.inAppEnabled").value(false));
    }

    @Test
    void shouldUpdateMyNotificationPreference() throws Exception {
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId("usr-1");
        preference.setEmailEnabled(false);
        preference.setInAppEnabled(true);

        when(notificationService.upsertNotificationPreference("usr-1", false, true))
                .thenReturn(preference);

        mockMvc.perform(patch("/api/notifications/preferences/me")
                        .header("X-User-Id", "usr-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailEnabled\":false,\"inAppEnabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("usr-1"))
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.inAppEnabled").value(true));
    }

    @Test
    void shouldReturnNotFoundWhenNotificationMissing() throws Exception {
        when(notificationService.markNotificationAsRead(99L, "usr-1"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        mockMvc.perform(patch("/api/notifications/99/read")
                        .header("X-User-Id", "usr-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"read\":true}"))
                .andExpect(status().isNotFound());
    }
}
