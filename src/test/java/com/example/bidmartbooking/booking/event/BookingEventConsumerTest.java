package com.example.bidmartbooking.booking.event;

import com.example.bidmartbooking.booking.model.Booking;
import com.example.bidmartbooking.booking.service.BookingService;
import com.example.bidmartbooking.booking.service.NotificationService;
import com.example.bidmartbooking.booking.service.ProcessedEventService;
import com.example.bidmartbooking.booking.service.ReliableEventProcessor;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingEventConsumerTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProcessedEventService processedEventService;

    @Mock
    private ReliableEventProcessor reliableEventProcessor;

    private BookingEventConsumer bookingEventConsumer;

    @BeforeEach
    void setUp() {
        bookingEventConsumer = new BookingEventConsumer(
                bookingService,
                notificationService,
                processedEventService,
                reliableEventProcessor
        );
        lenient().when(reliableEventProcessor.process(any(), any(), any()))
                .thenAnswer(invocation -> {
                    Supplier<?> action = invocation.getArgument(2);
                    return action.get();
                });
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
        verify(processedEventService).markProcessed("evt-1", "WinnerDetermined");
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
        verify(processedEventService).markProcessed("evt-1", "WinnerDetermined");
    }

    @Test
    void shouldSkipDuplicateWinnerEvent() {
        EventEnvelope<WinnerDeterminedPayload> event = buildValidEvent();
        when(processedEventService.hasProcessed("evt-1")).thenReturn(true);

        Booking result = bookingEventConsumer.handleWinnerDetermined(event);

        assertNull(result);
        verify(bookingService, never()).createBookingFromWinnerEvent(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
        verify(notificationService, never()).createWinLoseNotifications(
                any(), any(), any(), any()
        );
        verify(processedEventService, never()).markProcessed(any(), any());
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

    @Test
    void shouldCoverBidPlacedPayloadPojo() {
        BidPlacedPayload payload = new BidPlacedPayload();
        payload.setAuctionId("auc-bid");
        payload.setListingId("lst-bid");
        payload.setSellerUserId("seller-bid");
        payload.setBidderUserId("bidder-1");
        payload.setPreviousHighestBidderUserId("bidder-0");
        payload.setBidAmount(250000L);
        payload.setCurrency("IDR");
        payload.setItemName("Mouse");

        assertEquals("auc-bid", payload.getAuctionId());
        assertEquals("lst-bid", payload.getListingId());
        assertEquals("seller-bid", payload.getSellerUserId());
        assertEquals("bidder-1", payload.getBidderUserId());
        assertEquals("bidder-0", payload.getPreviousHighestBidderUserId());
        assertEquals(250000L, payload.getBidAmount());
        assertEquals("IDR", payload.getCurrency());
        assertEquals("Mouse", payload.getItemName());
    }

    @Test
    void shouldCoverBalanceConvertedPayloadPojo() {
        BalanceConvertedPayload payload = new BalanceConvertedPayload();
        payload.setBookingId("bkg-1");
        payload.setAuctionId("auc-1");
        payload.setUserId("buyer-1");
        payload.setAmount(500000L);
        payload.setCurrency("IDR");
        payload.setConversionReference("conv-1");

        assertEquals("bkg-1", payload.getBookingId());
        assertEquals("auc-1", payload.getAuctionId());
        assertEquals("buyer-1", payload.getUserId());
        assertEquals(500000L, payload.getAmount());
        assertEquals("IDR", payload.getCurrency());
        assertEquals("conv-1", payload.getConversionReference());
    }

    @Test
    void shouldCoverBalanceReleasedPayloadPojo() {
        BalanceReleasedPayload payload = new BalanceReleasedPayload();
        payload.setBookingId("bkg-2");
        payload.setAuctionId("auc-2");
        payload.setUserId("seller-1");
        payload.setAmount(500000L);
        payload.setCurrency("IDR");
        payload.setReleaseReference("rel-1");

        assertEquals("bkg-2", payload.getBookingId());
        assertEquals("auc-2", payload.getAuctionId());
        assertEquals("seller-1", payload.getUserId());
        assertEquals(500000L, payload.getAmount());
        assertEquals("IDR", payload.getCurrency());
        assertEquals("rel-1", payload.getReleaseReference());
    }

    @Test
    void shouldHandleAuctionClosedWhenNoWinner() {
        EventEnvelope<AuctionClosedPayload> event = buildValidAuctionClosedEvent();
        event.getPayload().setHasWinner(false);
        event.getPayload().setWinnerUserId(null);

        bookingEventConsumer.handleAuctionClosed(event);
        verify(processedEventService).markProcessed("evt-auction-closed-1", "AuctionClosed");
    }

    @Test
    void shouldHandleAuctionClosedWhenWinnerExists() {
        EventEnvelope<AuctionClosedPayload> event = buildValidAuctionClosedEvent();
        event.getPayload().setHasWinner(true);
        event.getPayload().setWinnerUserId("winner-1");

        bookingEventConsumer.handleAuctionClosed(event);
        verify(processedEventService).markProcessed("evt-auction-closed-1", "AuctionClosed");
    }

    @Test
    void shouldSkipDuplicateAuctionClosedEvent() {
        EventEnvelope<AuctionClosedPayload> event = buildValidAuctionClosedEvent();
        when(processedEventService.hasProcessed("evt-auction-closed-1")).thenReturn(true);

        bookingEventConsumer.handleAuctionClosed(event);

        verify(processedEventService, never()).markProcessed(any(), any());
    }

    @Test
    void shouldThrowWhenAuctionClosedEventInvalid() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleAuctionClosed(null)
        );
        assertEquals("Invalid event: payload is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAuctionClosedPayloadNull() {
        EventEnvelope<AuctionClosedPayload> event = new EventEnvelope<>();
        event.setEventId("evt-auction-null-payload");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleAuctionClosed(event)
        );
        assertEquals("Invalid event: payload is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAuctionClosedMissingHasWinner() {
        EventEnvelope<AuctionClosedPayload> event = buildValidAuctionClosedEvent();
        event.getPayload().setHasWinner(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleAuctionClosed(event)
        );
        assertEquals("hasWinner is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAuctionClosedHasWinnerButWinnerIdMissing() {
        EventEnvelope<AuctionClosedPayload> event = buildValidAuctionClosedEvent();
        event.getPayload().setHasWinner(true);
        event.getPayload().setWinnerUserId(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleAuctionClosed(event)
        );
        assertEquals("winnerUserId is required when hasWinner=true", exception.getMessage());
    }

    @Test
    void shouldHandleBidPlacedEvent() {
        EventEnvelope<BidPlacedPayload> event = buildValidBidPlacedEvent();

        bookingEventConsumer.handleBidPlaced(event);

        verify(notificationService).createBidPlacedNotifications(
                "seller-1",
                "bidder-2",
                "bidder-1",
                "auc-bid",
                250000L,
                "Keyboard"
        );
        verify(processedEventService).markProcessed("evt-bid-1", "BidPlaced");
    }

    @Test
    void shouldSkipDuplicateBidPlacedEvent() {
        EventEnvelope<BidPlacedPayload> event = buildValidBidPlacedEvent();
        when(processedEventService.hasProcessed("evt-bid-1")).thenReturn(true);

        bookingEventConsumer.handleBidPlaced(event);

        verify(notificationService, never()).createBidPlacedNotifications(
                any(), any(), any(), any(), any(), any()
        );
        verify(processedEventService, never()).markProcessed(any(), any());
    }

    @Test
    void shouldThrowWhenBidPlacedEventIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBidPlaced(null)
        );

        assertEquals("Invalid event: payload is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBidPlacedPayloadIsNull() {
        EventEnvelope<BidPlacedPayload> event = new EventEnvelope<>();
        event.setEventId("evt-bid-null-payload");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBidPlaced(event)
        );

        assertEquals("Invalid event: payload is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBidPlacedEventIdBlank() {
        EventEnvelope<BidPlacedPayload> event = buildValidBidPlacedEvent();
        event.setEventId(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBidPlaced(event)
        );

        assertEquals("eventId is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBidPlacedAuctionIdBlank() {
        EventEnvelope<BidPlacedPayload> event = buildValidBidPlacedEvent();
        event.getPayload().setAuctionId(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBidPlaced(event)
        );

        assertEquals("auctionId is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBidPlacedListingIdBlank() {
        EventEnvelope<BidPlacedPayload> event = buildValidBidPlacedEvent();
        event.getPayload().setListingId(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBidPlaced(event)
        );

        assertEquals("listingId is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBidPlacedSellerUserIdBlank() {
        EventEnvelope<BidPlacedPayload> event = buildValidBidPlacedEvent();
        event.getPayload().setSellerUserId(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBidPlaced(event)
        );

        assertEquals("sellerUserId is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBidPlacedBidderUserIdBlank() {
        EventEnvelope<BidPlacedPayload> event = buildValidBidPlacedEvent();
        event.getPayload().setBidderUserId(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBidPlaced(event)
        );

        assertEquals("bidderUserId is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBidPlacedAmountInvalid() {
        EventEnvelope<BidPlacedPayload> event = buildValidBidPlacedEvent();
        event.getPayload().setBidAmount(-1L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBidPlaced(event)
        );

        assertEquals("bidAmount must be >= 0", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBidPlacedAmountNull() {
        EventEnvelope<BidPlacedPayload> event = buildValidBidPlacedEvent();
        event.getPayload().setBidAmount(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBidPlaced(event)
        );

        assertEquals("bidAmount must be >= 0", exception.getMessage());
    }

    @Test
    void shouldHandleBalanceConvertedEvent() {
        EventEnvelope<BalanceConvertedPayload> event = buildValidBalanceConvertedEvent();

        bookingEventConsumer.handleBalanceConverted(event);

        verify(notificationService).createBalanceConvertedNotification(
                "buyer-1",
                "auc-pay",
                500000L
        );
        verify(processedEventService).markProcessed("evt-balance-converted-1", "BalanceConverted");
    }

    @Test
    void shouldHandleBalanceReleasedEvent() {
        EventEnvelope<BalanceReleasedPayload> event = buildValidBalanceReleasedEvent();

        bookingEventConsumer.handleBalanceReleased(event);

        verify(notificationService).createBalanceReleasedNotification(
                "seller-1",
                "auc-release",
                500000L
        );
        verify(processedEventService).markProcessed("evt-balance-released-1", "BalanceReleased");
    }

    @Test
    void shouldSkipDuplicateBalanceConvertedEvent() {
        EventEnvelope<BalanceConvertedPayload> event = buildValidBalanceConvertedEvent();
        when(processedEventService.hasProcessed("evt-balance-converted-1")).thenReturn(true);

        bookingEventConsumer.handleBalanceConverted(event);

        verify(notificationService, never()).createBalanceConvertedNotification(
                any(), any(), any()
        );
        verify(processedEventService, never()).markProcessed(any(), any());
    }

    @Test
    void shouldSkipDuplicateBalanceReleasedEvent() {
        EventEnvelope<BalanceReleasedPayload> event = buildValidBalanceReleasedEvent();
        when(processedEventService.hasProcessed("evt-balance-released-1")).thenReturn(true);

        bookingEventConsumer.handleBalanceReleased(event);

        verify(notificationService, never()).createBalanceReleasedNotification(
                any(), any(), any()
        );
        verify(processedEventService, never()).markProcessed(any(), any());
    }

    @Test
    void shouldThrowWhenBalanceConvertedEventIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBalanceConverted(null)
        );

        assertEquals("Invalid event: payload is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBalanceConvertedPayloadIsNull() {
        EventEnvelope<BalanceConvertedPayload> event = new EventEnvelope<>();
        event.setEventId("evt-balance-converted-null");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBalanceConverted(event)
        );

        assertEquals("Invalid event: payload is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBalanceConvertedUserIdBlank() {
        EventEnvelope<BalanceConvertedPayload> event = buildValidBalanceConvertedEvent();
        event.getPayload().setUserId(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBalanceConverted(event)
        );

        assertEquals("userId is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBalanceConvertedAmountNull() {
        EventEnvelope<BalanceConvertedPayload> event = buildValidBalanceConvertedEvent();
        event.getPayload().setAmount(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBalanceConverted(event)
        );

        assertEquals("amount must be >= 0", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBalanceConvertedAmountInvalid() {
        EventEnvelope<BalanceConvertedPayload> event = buildValidBalanceConvertedEvent();
        event.getPayload().setAmount(-1L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBalanceConverted(event)
        );

        assertEquals("amount must be >= 0", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBalanceReleasedEventIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBalanceReleased(null)
        );

        assertEquals("Invalid event: payload is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBalanceReleasedPayloadIsNull() {
        EventEnvelope<BalanceReleasedPayload> event = new EventEnvelope<>();
        event.setEventId("evt-balance-released-null");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBalanceReleased(event)
        );

        assertEquals("Invalid event: payload is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBalanceReleasedUserIdBlank() {
        EventEnvelope<BalanceReleasedPayload> event = buildValidBalanceReleasedEvent();
        event.getPayload().setUserId(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBalanceReleased(event)
        );

        assertEquals("userId is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBalanceReleasedAmountNull() {
        EventEnvelope<BalanceReleasedPayload> event = buildValidBalanceReleasedEvent();
        event.getPayload().setAmount(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBalanceReleased(event)
        );

        assertEquals("amount must be >= 0", exception.getMessage());
    }

    @Test
    void shouldThrowWhenBalanceReleasedAmountInvalid() {
        EventEnvelope<BalanceReleasedPayload> event = buildValidBalanceReleasedEvent();
        event.getPayload().setAmount(-1L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleBalanceReleased(event)
        );

        assertEquals("amount must be >= 0", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAuctionClosedEventIdBlank() {
        EventEnvelope<AuctionClosedPayload> event = buildValidAuctionClosedEvent();
        event.setEventId(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleAuctionClosed(event)
        );
        assertEquals("eventId is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAuctionClosedAuctionIdBlank() {
        EventEnvelope<AuctionClosedPayload> event = buildValidAuctionClosedEvent();
        event.getPayload().setAuctionId(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleAuctionClosed(event)
        );
        assertEquals("auctionId is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAuctionClosedListingIdBlank() {
        EventEnvelope<AuctionClosedPayload> event = buildValidAuctionClosedEvent();
        event.getPayload().setListingId(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleAuctionClosed(event)
        );
        assertEquals("listingId is required", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAuctionClosedWinnerIdNullAndHasWinnerTrue() {
        EventEnvelope<AuctionClosedPayload> event = buildValidAuctionClosedEvent();
        event.getPayload().setHasWinner(true);
        event.getPayload().setWinnerUserId(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.handleAuctionClosed(event)
        );
        assertEquals("winnerUserId is required when hasWinner=true", exception.getMessage());
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

    private EventEnvelope<AuctionClosedPayload> buildValidAuctionClosedEvent() {
        EventEnvelope<AuctionClosedPayload> event = new EventEnvelope<>();
        event.setEventId("evt-auction-closed-1");

        AuctionClosedPayload payload = new AuctionClosedPayload();
        payload.setAuctionId("auc-closed-1");
        payload.setListingId("lst-closed-1");
        payload.setClosedAt("2026-03-05T00:00:00Z");
        payload.setHasWinner(false);
        payload.setWinnerUserId(null);

        event.setPayload(payload);
        return event;
    }

    private EventEnvelope<BidPlacedPayload> buildValidBidPlacedEvent() {
        EventEnvelope<BidPlacedPayload> event = new EventEnvelope<>();
        event.setEventId("evt-bid-1");

        BidPlacedPayload payload = new BidPlacedPayload();
        payload.setAuctionId("auc-bid");
        payload.setListingId("lst-bid");
        payload.setSellerUserId("seller-1");
        payload.setBidderUserId("bidder-2");
        payload.setPreviousHighestBidderUserId("bidder-1");
        payload.setBidAmount(250000L);
        payload.setCurrency("IDR");
        payload.setItemName("Keyboard");

        event.setPayload(payload);
        return event;
    }

    private EventEnvelope<BalanceConvertedPayload> buildValidBalanceConvertedEvent() {
        EventEnvelope<BalanceConvertedPayload> event = new EventEnvelope<>();
        event.setEventId("evt-balance-converted-1");

        BalanceConvertedPayload payload = new BalanceConvertedPayload();
        payload.setBookingId("bkg-1");
        payload.setAuctionId("auc-pay");
        payload.setUserId("buyer-1");
        payload.setAmount(500000L);
        payload.setCurrency("IDR");
        payload.setConversionReference("conv-1");

        event.setPayload(payload);
        return event;
    }

    private EventEnvelope<BalanceReleasedPayload> buildValidBalanceReleasedEvent() {
        EventEnvelope<BalanceReleasedPayload> event = new EventEnvelope<>();
        event.setEventId("evt-balance-released-1");

        BalanceReleasedPayload payload = new BalanceReleasedPayload();
        payload.setBookingId("bkg-2");
        payload.setAuctionId("auc-release");
        payload.setUserId("seller-1");
        payload.setAmount(500000L);
        payload.setCurrency("IDR");
        payload.setReleaseReference("rel-1");

        event.setPayload(payload);
        return event;
    }
}
