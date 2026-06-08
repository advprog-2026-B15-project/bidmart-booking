package com.example.bidmartbooking.booking.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
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

        listener.onWinnerDetermined(message(json));

        verify(consumer).handleWinnerDetermined(any());
    }

    @Test
    void onWinnerDetermined_invalidJson_logsErrorWithoutThrowing() {
        listener.onWinnerDetermined(message("not-valid-json"));

        verify(consumer, never()).handleWinnerDetermined(any());
    }

    @Test
    void onAuctionClosed_validMessage_callsConsumer() throws Exception {
        String json = "{\"eventId\":\"evt-2\",\"eventType\":\"AuctionClosed\","
                + "\"payload\":{\"auctionId\":\"a1\",\"listingId\":\"l1\","
                + "\"hasWinner\":false,\"closedAt\":\"2026-05-22T10:00:00Z\"}}";

        listener.onAuctionClosed(message(json));

        verify(consumer).handleAuctionClosed(any());
    }

    @Test
    void onAuctionClosed_invalidJson_logsErrorWithoutThrowing() {
        listener.onAuctionClosed(message("{bad}"));

        verify(consumer, never()).handleAuctionClosed(any());
    }

    @Test
    void onBidPlaced_validMessage_callsConsumer() throws Exception {
        String json = "{\"eventId\":\"evt-3\",\"eventType\":\"BidPlaced\","
                + "\"payload\":{\"auctionId\":\"a1\",\"listingId\":\"l1\","
                + "\"sellerUserId\":\"s1\",\"bidderUserId\":\"b1\","
                + "\"bidAmount\":50000,\"itemName\":\"Item\"}}";

        listener.onBidPlaced(message(json));

        verify(consumer).handleBidPlaced(any());
    }

    @Test
    void onBidPlaced_invalidJson_logsErrorWithoutThrowing() {
        listener.onBidPlaced(message("{bad}"));

        verify(consumer, never()).handleBidPlaced(any());
    }

    @Test
    void onBalanceConverted_rawWalletMessage_callsConsumer() throws Exception {
        String json = "{\"userId\":\"u1\",\"auctionId\":\"a1\","
                + "\"bookingId\":\"1\",\"amount\":100000,\"currency\":\"IDR\"}";

        listener.onBalanceConverted(message(json));

        verify(consumer).handleBalanceConverted(any());
    }

    @Test
    void onBalanceConverted_invalidJson_logsErrorWithoutThrowing() {
        listener.onBalanceConverted(message("not-json"));

        verify(consumer, never()).handleBalanceConverted(any());
    }

    @Test
    void onBalanceReleased_rawWalletMessage_callsConsumer() throws Exception {
        String json = "{\"userId\":\"u1\",\"auctionId\":\"a1\","
                + "\"bookingId\":\"1\",\"amount\":100000,\"currency\":\"IDR\"}";

        listener.onBalanceReleased(message(json));

        verify(consumer).handleBalanceReleased(any());
    }

    @Test
    void onBalanceReleased_invalidJson_logsErrorWithoutThrowing() {
        listener.onBalanceReleased(message("not-json"));

        verify(consumer, never()).handleBalanceReleased(any());
    }

    private Message message(String body) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }
}
