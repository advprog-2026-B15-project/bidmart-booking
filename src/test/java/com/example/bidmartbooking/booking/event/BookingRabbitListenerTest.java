package com.example.bidmartbooking.booking.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingRabbitListenerTest {

    @Mock
    private BookingEventConsumer consumer;

    private BookingRabbitListener listener;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        listener = new BookingRabbitListener(consumer, mapper);
    }

    @Test
    void onWinnerDetermined_validMessage_callsConsumer() throws Exception {
        String json = "{\"eventId\":\"evt-1\",\"eventType\":\"WinnerDetermined\","
                + "\"payload\":{\"auctionId\":\"a1\",\"listingId\":\"l1\","
                + "\"sellerUserId\":\"s1\",\"winnerUserId\":\"w1\","
                + "\"finalPrice\":100000,\"currency\":\"IDR\","
                + "\"itemName\":\"Item\",\"quantity\":1,\"loserUserIds\":[]}}";

        listener.onWinnerDetermined(json);

        verify(consumer).handleWinnerDetermined(any());
    }

    @Test
    void onWinnerDetermined_invalidJson_logsErrorWithoutThrowing() {
        listener.onWinnerDetermined("not-valid-json");

        verify(consumer, never()).handleWinnerDetermined(any());
    }

    @Test
    void onAuctionClosed_validMessage_callsConsumer() throws Exception {
        String json = "{\"eventId\":\"evt-2\",\"eventType\":\"AuctionClosed\","
                + "\"payload\":{\"auctionId\":\"a1\",\"listingId\":\"l1\","
                + "\"hasWinner\":false,\"closedAt\":\"2026-05-22T10:00:00Z\"}}";

        listener.onAuctionClosed(json);

        verify(consumer).handleAuctionClosed(any());
    }

    @Test
    void onAuctionClosed_invalidJson_logsErrorWithoutThrowing() {
        listener.onAuctionClosed("{bad}");

        verify(consumer, never()).handleAuctionClosed(any());
    }

    @Test
    void onBidPlaced_validMessage_callsConsumer() throws Exception {
        String json = "{\"eventId\":\"evt-3\",\"eventType\":\"BidPlaced\","
                + "\"payload\":{\"auctionId\":\"a1\",\"listingId\":\"l1\","
                + "\"sellerUserId\":\"s1\",\"bidderUserId\":\"b1\","
                + "\"bidAmount\":50000,\"itemName\":\"Item\"}}";

        listener.onBidPlaced(json);

        verify(consumer).handleBidPlaced(any());
    }

    @Test
    void onBidPlaced_invalidJson_logsErrorWithoutThrowing() {
        listener.onBidPlaced(null);

        verify(consumer, never()).handleBidPlaced(any());
    }

    @Test
    void onBalanceConverted_validMessage_callsConsumer() throws Exception {
        String json = "{\"eventId\":\"evt-4\",\"eventType\":\"BalanceConverted\","
                + "\"payload\":{\"userId\":\"u1\",\"auctionId\":\"a1\","
                + "\"bookingId\":\"1\",\"amount\":100000,\"currency\":\"IDR\"}}";

        listener.onBalanceConverted(json);

        verify(consumer).handleBalanceConverted(any());
    }

    @Test
    void onBalanceConverted_invalidJson_logsErrorWithoutThrowing() {
        listener.onBalanceConverted("not-json");

        verify(consumer, never()).handleBalanceConverted(any());
    }

    @Test
    void onBalanceReleased_validMessage_callsConsumer() throws Exception {
        String json = "{\"eventId\":\"evt-5\",\"eventType\":\"BalanceReleased\","
                + "\"payload\":{\"userId\":\"u1\",\"auctionId\":\"a1\","
                + "\"bookingId\":\"1\",\"amount\":100000,\"currency\":\"IDR\"}}";

        listener.onBalanceReleased(json);

        verify(consumer).handleBalanceReleased(any());
    }

    @Test
    void onBalanceReleased_invalidJson_logsErrorWithoutThrowing() {
        listener.onBalanceReleased("not-json");

        verify(consumer, never()).handleBalanceReleased(any());
    }
}
