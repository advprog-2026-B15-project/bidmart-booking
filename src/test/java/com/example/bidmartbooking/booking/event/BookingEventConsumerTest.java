package com.example.bidmartbooking.booking.event;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.service.BookingService;
import com.example.bidmartbooking.booking.service.NotificationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingEventConsumerTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private NotificationService notificationService;

    private BookingEventConsumer bookingEventConsumer;

    @BeforeEach
    void setUp() {
        bookingEventConsumer = new BookingEventConsumer(bookingService, notificationService);
    }

    @Test
    void shouldHandleWinnerEventAndUseDefaultCurrencyAndEmptyLosers() {
        EventEnvelope<WinnerDeterminedPayload> event = buildValidEvent();
        event.getPayload().setCurrency(null);
        event.getPayload().setLoserUserIds(null);

        Booking booking = new Booking();
        booking.setId(88L);
        when(bookingService.createBookingFromWinnerEvent(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(booking);

        Booking result = bookingEventConsumer.handleWinnerDetermined(event);

        assertEquals(88L, result.getId());

        ArgumentCaptor<List<String>> losersCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).createWinLoseNotifications(
                any(), losersCaptor.capture(), any(), any()
        );
        assertEquals(0, losersCaptor.getValue().size());
    }

    @Test
    void shouldThrowWhenEventIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleWinnerDetermined(null)
        );

        assertEquals("Invalid event: payload is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenPayloadIsNull() {
        EventEnvelope<WinnerDeterminedPayload> event = new EventEnvelope<>();
        event.setEventId("evt-1");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleWinnerDetermined(event)
        );

        assertEquals("Invalid event: payload is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenEventIdBlank() {
        EventEnvelope<WinnerDeterminedPayload> event = buildValidEvent();
        event.setEventId(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleWinnerDetermined(event)
        );

        assertEquals("eventId is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenEventIdNull() {
        EventEnvelope<WinnerDeterminedPayload> event = buildValidEvent();
        event.setEventId(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleWinnerDetermined(event)
        );

        assertEquals("eventId is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAuctionIdBlank() {
        EventEnvelope<WinnerDeterminedPayload> event = buildValidEvent();
        event.getPayload().setAuctionId(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleWinnerDetermined(event)
        );

        assertEquals("auctionId is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenListingIdBlank() {
        EventEnvelope<WinnerDeterminedPayload> event = buildValidEvent();
        event.getPayload().setListingId(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleWinnerDetermined(event)
        );

        assertEquals("listingId is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenSellerUserIdBlank() {
        EventEnvelope<WinnerDeterminedPayload> event = buildValidEvent();
        event.getPayload().setSellerUserId(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleWinnerDetermined(event)
        );

        assertEquals("sellerUserId is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenWinnerUserIdBlank() {
        EventEnvelope<WinnerDeterminedPayload> event = buildValidEvent();
        event.getPayload().setWinnerUserId(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleWinnerDetermined(event)
        );

        assertEquals("winnerUserId is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenFinalPriceInvalid() {
        EventEnvelope<WinnerDeterminedPayload> event = buildValidEvent();
        event.getPayload().setFinalPrice(-1L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleWinnerDetermined(event)
        );

        assertEquals("finalPrice must be >= 0", exception.getMessage());
    }

    @Test
    void shouldThrowWhenFinalPriceNull() {
        EventEnvelope<WinnerDeterminedPayload> event = buildValidEvent();
        event.getPayload().setFinalPrice(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleWinnerDetermined(event)
        );

        assertEquals("finalPrice must be >= 0", exception.getMessage());
    }

    @Test
    void shouldHandleWinnerEventWithLosers() {
        EventEnvelope<WinnerDeterminedPayload> event = buildValidEvent();

        Booking booking = new Booking();
        booking.setId(99L);
        when(bookingService.createBookingFromWinnerEvent(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(booking);

        Booking result = bookingEventConsumer.handleWinnerDetermined(event);

        assertNotNull(result);
        verify(notificationService).createWinLoseNotifications(
                "winner-1",
                List.of("loser-1", "loser-2"),
                "auc-1",
                5000L
        );
    }

    @Test
    void shouldCoverAuctionClosedPayloadPojo() {
        AuctionClosedPayload payload = new AuctionClosedPayload();
        payload.setAuctionId("auc-closed");
        payload.setListingId("lst-closed");
        payload.setClosedAt("2026-03-05T00:00:00Z");
        payload.setHasWinner(true);
        payload.setWinnerUserId("winner-1");

        assertEquals("auc-closed", payload.getAuctionId());
        assertEquals("lst-closed", payload.getListingId());
        assertEquals("2026-03-05T00:00:00Z", payload.getClosedAt());
        assertEquals(true, payload.getHasWinner());
        assertEquals("winner-1", payload.getWinnerUserId());
    }

    private EventEnvelope<WinnerDeterminedPayload> buildValidEvent() {
        EventEnvelope<WinnerDeterminedPayload> event = new EventEnvelope<>();
        event.setEventId("evt-1");

        WinnerDeterminedPayload payload = new WinnerDeterminedPayload();
        payload.setAuctionId("auc-1");
        payload.setListingId("lst-1");
        payload.setSellerUserId("seller-1");
        payload.setWinnerUserId("winner-1");
        payload.setFinalPrice(5000L);
        payload.setCurrency("IDR");
        payload.setItemName("Item");
        payload.setQuantity(1);
        payload.setLoserUserIds(List.of("loser-1", "loser-2"));

        event.setPayload(payload);
        return event;
    }
}
