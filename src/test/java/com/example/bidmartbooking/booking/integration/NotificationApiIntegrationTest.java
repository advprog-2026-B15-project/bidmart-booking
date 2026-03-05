package com.example.bidmartbooking.booking.integration;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.model.BookingStatus;
import com.example.bidmartbooking.booking.model.Notification;
import com.example.bidmartbooking.booking.model.NotificationType;
import com.example.bidmartbooking.booking.repository.BookingRepository;
import com.example.bidmartbooking.booking.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    void shouldGetNotificationsForCurrentUser() throws Exception {
        createNotification("usr-a", NotificationType.WIN, false, null);
        createNotification("usr-b", NotificationType.LOSE, false, null);

        mockMvc.perform(get("/api/notifications/me").header("X-User-Id", "usr-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("WIN"));
    }

    @Test
    void shouldGetNotificationWithRelatedBookingId() throws Exception {
        Booking booking = createBooking("auc-notif", "usr-a", "seller-a", 500000L);
        Notification notification = createNotification("usr-a", NotificationType.WIN, false, booking);

        mockMvc.perform(get("/api/notifications/me").header("X-User-Id", "usr-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(notification.getId()))
                .andExpect(jsonPath("$[0].relatedBookingId").value(booking.getId()));
    }

    @Test
    void shouldMarkNotificationAsRead() throws Exception {
        Notification notification = createNotification("usr-a", NotificationType.WIN, false, null);

        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId())
                        .header("X-User-Id", "usr-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"read\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    void shouldRejectMarkReadWhenReadFalse() throws Exception {
        Notification notification = createNotification("usr-a", NotificationType.WIN, false, null);

        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId())
                        .header("X-User-Id", "usr-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"read\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnNotFoundWhenMarkReadNotOwned() throws Exception {
        Notification notification = createNotification("usr-owner", NotificationType.WIN, false, null);

        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId())
                        .header("X-User-Id", "usr-other")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"read\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private Booking createBooking(String auctionId, String buyer, String seller, Long total) {
        Booking booking = new Booking();
        booking.setSourceEventId("evt-" + auctionId);
        booking.setAuctionId(auctionId);
        booking.setListingId("lst-" + auctionId);
        booking.setBuyerUserId(buyer);
        booking.setSellerUserId(seller);
        booking.setStatus(BookingStatus.CREATED);
        booking.setTotalAmount(total);
        booking.setCurrency("IDR");
        return bookingRepository.save(booking);
    }

    private Notification createNotification(
            String userId,
            NotificationType type,
            boolean isRead,
            Booking relatedBooking
    ) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(type.name() + " title");
        notification.setMessage(type.name() + " message");
        notification.setIsRead(isRead);
        notification.setRelatedAuctionId("auc-notif");
        notification.setRelatedBooking(relatedBooking);
        return notificationRepository.save(notification);
    }
}
