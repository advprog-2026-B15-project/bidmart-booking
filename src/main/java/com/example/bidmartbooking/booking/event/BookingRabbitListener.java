package com.example.bidmartbooking.booking.event;

import com.example.bidmartbooking.config.RabbitMQConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingRabbitListener {

    private final BookingEventConsumer consumer;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_WINNER_DETERMINED)
    public void onWinnerDetermined(Message message) {
        try {
            EventEnvelope<WinnerDeterminedPayload> event = readEventEnvelope(
                    message, new TypeReference<EventEnvelope<WinnerDeterminedPayload>>() { }
            );
            consumer.handleWinnerDetermined(event);
        } catch (Exception e) {
            log.error("[WinnerDetermined] Failed to process message: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_AUCTION_CLOSED)
    public void onAuctionClosed(Message message) {
        try {
            EventEnvelope<AuctionClosedPayload> event = readEventEnvelope(
                    message, new TypeReference<EventEnvelope<AuctionClosedPayload>>() { }
            );
            consumer.handleAuctionClosed(event);
        } catch (Exception e) {
            log.error("[AuctionClosed] Failed to process message: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BID_PLACED)
    public void onBidPlaced(Message message) {
        try {
            EventEnvelope<BidPlacedPayload> event = readEventEnvelope(
                    message, new TypeReference<EventEnvelope<BidPlacedPayload>>() { }
            );
            consumer.handleBidPlaced(event);
        } catch (Exception e) {
            log.error("[BidPlaced] Failed to process message: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BALANCE_CONVERTED)
    public void onBalanceConverted(Message message) {
        try {
            EventEnvelope<BalanceConvertedPayload> event = readWalletEnvelope(
                    message,
                    "BalanceConverted",
                    new TypeReference<EventEnvelope<BalanceConvertedPayload>>() { },
                    new TypeReference<BalanceConvertedPayload>() { }
            );
            consumer.handleBalanceConverted(event);
        } catch (Exception e) {
            log.error("[BalanceConverted] Failed to process message: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BALANCE_RELEASED)
    public void onBalanceReleased(Message message) {
        try {
            EventEnvelope<BalanceReleasedPayload> event = readWalletEnvelope(
                    message,
                    "BalanceReleased",
                    new TypeReference<EventEnvelope<BalanceReleasedPayload>>() { },
                    new TypeReference<BalanceReleasedPayload>() { }
            );
            consumer.handleBalanceReleased(event);
        } catch (Exception e) {
            log.error("[BalanceReleased] Failed to process message: {}", e.getMessage());
        }
    }

    private <T> EventEnvelope<T> readEventEnvelope(Message message, TypeReference<EventEnvelope<T>> typeReference)
            throws Exception {
        return objectMapper.readValue(readBody(message), typeReference);
    }

    private <T> EventEnvelope<T> readWalletEnvelope(
            Message message,
            String eventType,
            TypeReference<EventEnvelope<T>> envelopeType,
            TypeReference<T> payloadType
    ) throws Exception {
        String body = readBody(message);
        Map<String, Object> root = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() { });
        if (root.containsKey("payload")) {
            return objectMapper.readValue(body, envelopeType);
        }

        T payload = objectMapper.readValue(body, payloadType);
        EventEnvelope<T> envelope = new EventEnvelope<>();
        envelope.setEventType(eventType);
        envelope.setEventVersion(1);
        envelope.setSource("bidmart-wallet");
        envelope.setEventId(buildSyntheticEventId(eventType, root));
        envelope.setPayload(payload);
        return envelope;
    }

    private String readBody(Message message) {
        return new String(message.getBody(), StandardCharsets.UTF_8);
    }

    private String buildSyntheticEventId(String eventType, Map<String, Object> payload) {
        Object auctionId = payload.getOrDefault("auctionId", "unknown-auction");
        Object userId = payload.getOrDefault("userId", "unknown-user");
        Object amount = payload.getOrDefault("amount", "unknown-amount");
        return eventType + ":" + auctionId + ":" + userId + ":" + amount;
    }
}
