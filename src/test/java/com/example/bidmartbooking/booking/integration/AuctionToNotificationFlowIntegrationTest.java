package com.example.bidmartbooking.booking.integration;

import com.example.bidmartbooking.booking.event.AuctionClosedPayload;
import com.example.bidmartbooking.booking.event.BookingEventConsumer;
import com.example.bidmartbooking.booking.event.EventEnvelope;
import com.example.bidmartbooking.booking.event.WinnerDeterminedPayload;
import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.repository.BookingItemRepository;
import com.example.bidmartbooking.booking.repository.BookingRepository;
import com.example.bidmartbooking.booking.repository.BookingStatusAuditLogRepository;
import com.example.bidmartbooking.booking.repository.DeadLetterEventRepository;
import com.example.bidmartbooking.booking.repository.NotificationRepository;
import com.example.bidmartbooking.booking.repository.ProcessedEventRepository;
import com.example.bidmartbooking.booking.repository.ShipmentRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuctionToNotificationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingEventConsumer bookingEventConsumer;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingItemRepository bookingItemRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private DeadLetterEventRepository deadLetterEventRepository;

    @Autowired
    private BookingStatusAuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        shipmentRepository.deleteAll();
        bookingItemRepository.deleteAll();
        auditLogRepository.deleteAll();
        bookingRepository.deleteAll();
        processedEventRepository.deleteAll();
        deadLetterEventRepository.deleteAll();
    }

    @Test
    void shouldCreateBookingAndNotificationsFromAuctionEvents() throws Exception {
        bookingEventConsumer.handleAuctionClosed(buildAuctionClosedEvent());

        Booking booking = bookingEventConsumer.handleWinnerDetermined(
                buildWinnerDeterminedEvent()
        );

        assertNotNull(booking);
        assertEquals(1L, bookingRepository.count());
        assertEquals(1L, bookingItemRepository.count());
        assertEquals(1L, shipmentRepository.count());
        assertEquals(3L, notificationRepository.count());
        assertTrue(processedEventRepository.existsByEventId("evt-auction-closed-e2e"));
        assertTrue(processedEventRepository.existsByEventId("evt-winner-e2e"));
        assertEquals(1, auditLogRepository
                .findByBookingIdOrderByCreatedAtDesc(booking.getId())
                .size());

        mockMvc.perform(get("/api/bookings/me").header("X-User-Id", "winner-e2e"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].auctionId").value("auc-e2e"));

        mockMvc.perform(get("/api/notifications/me").header("X-User-Id", "winner-e2e"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("WIN"));

        mockMvc.perform(get("/api/notifications/me").header("X-User-Id", "loser-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("LOSE"));
    }

    @Test
    void shouldSkipDuplicateWinnerEventInFullFlow() {
        Booking firstBooking = bookingEventConsumer.handleWinnerDetermined(
                buildWinnerDeterminedEvent()
        );
        Booking duplicateBooking = bookingEventConsumer.handleWinnerDetermined(
                buildWinnerDeterminedEvent()
        );

        assertNotNull(firstBooking);
        assertNull(duplicateBooking);
        assertEquals(1L, bookingRepository.count());
        assertEquals(3L, notificationRepository.count());
        assertEquals(1L, processedEventRepository.count());
        assertEquals(0L, deadLetterEventRepository.count());
    }

    private EventEnvelope<AuctionClosedPayload> buildAuctionClosedEvent() {
        AuctionClosedPayload payload = new AuctionClosedPayload();
        payload.setAuctionId("auc-e2e");
        payload.setListingId("lst-e2e");
        payload.setClosedAt("2026-03-04T09:20:00Z");
        payload.setHasWinner(true);
        payload.setWinnerUserId("winner-e2e");

        EventEnvelope<AuctionClosedPayload> event = new EventEnvelope<>();
        event.setEventId("evt-auction-closed-e2e");
        event.setEventType("AuctionClosed");
        event.setEventVersion(1);
        event.setOccurredAt("2026-03-04T09:20:00Z");
        event.setSource("bidmart-auction");
        event.setPayload(payload);
        return event;
    }

    private EventEnvelope<WinnerDeterminedPayload> buildWinnerDeterminedEvent() {
        WinnerDeterminedPayload payload = new WinnerDeterminedPayload();
        payload.setAuctionId("auc-e2e");
        payload.setListingId("lst-e2e");
        payload.setSellerUserId("seller-e2e");
        payload.setWinnerUserId("winner-e2e");
        payload.setFinalPrice(1500000L);
        payload.setCurrency("IDR");
        payload.setItemName("Mechanical Keyboard");
        payload.setQuantity(1);
        payload.setLoserUserIds(List.of("loser-a", "loser-b"));

        EventEnvelope<WinnerDeterminedPayload> event = new EventEnvelope<>();
        event.setEventId("evt-winner-e2e");
        event.setEventType("WinnerDetermined");
        event.setEventVersion(1);
        event.setOccurredAt("2026-03-04T09:21:00Z");
        event.setSource("bidmart-auction");
        event.setPayload(payload);
        return event;
    }
}
