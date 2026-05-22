package com.example.bidmartbooking.booking.event;

import com.example.bidmartbooking.config.RabbitMQConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingRabbitListener {

    private final BookingEventConsumer consumer;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_WINNER_DETERMINED)
    public void onWinnerDetermined(String message) {
        try {
            EventEnvelope<WinnerDeterminedPayload> event = objectMapper.readValue(
                    message, new TypeReference<EventEnvelope<WinnerDeterminedPayload>>() { });
            consumer.handleWinnerDetermined(event);
        } catch (Exception e) {
            log.error("[WinnerDetermined] Failed to process message: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_AUCTION_CLOSED)
    public void onAuctionClosed(String message) {
        try {
            EventEnvelope<AuctionClosedPayload> event = objectMapper.readValue(
                    message, new TypeReference<EventEnvelope<AuctionClosedPayload>>() { });
            consumer.handleAuctionClosed(event);
        } catch (Exception e) {
            log.error("[AuctionClosed] Failed to process message: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BID_PLACED)
    public void onBidPlaced(String message) {
        try {
            EventEnvelope<BidPlacedPayload> event = objectMapper.readValue(
                    message, new TypeReference<EventEnvelope<BidPlacedPayload>>() { });
            consumer.handleBidPlaced(event);
        } catch (Exception e) {
            log.error("[BidPlaced] Failed to process message: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BALANCE_CONVERTED)
    public void onBalanceConverted(String message) {
        try {
            EventEnvelope<BalanceConvertedPayload> event = objectMapper.readValue(
                    message, new TypeReference<EventEnvelope<BalanceConvertedPayload>>() { });
            consumer.handleBalanceConverted(event);
        } catch (Exception e) {
            log.error("[BalanceConverted] Failed to process message: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_BALANCE_RELEASED)
    public void onBalanceReleased(String message) {
        try {
            EventEnvelope<BalanceReleasedPayload> event = objectMapper.readValue(
                    message, new TypeReference<EventEnvelope<BalanceReleasedPayload>>() { });
            consumer.handleBalanceReleased(event);
        } catch (Exception e) {
            log.error("[BalanceReleased] Failed to process message: {}", e.getMessage());
        }
    }
}
