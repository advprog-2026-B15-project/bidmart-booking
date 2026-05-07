package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.event.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class ReliableEventProcessor {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final DeadLetterEventService deadLetterEventService;
    private final ObjectMapper objectMapper;

    public ReliableEventProcessor(
            DeadLetterEventService deadLetterEventService,
            ObjectMapper objectMapper
    ) {
        this.deadLetterEventService = deadLetterEventService;
        this.objectMapper = objectMapper;
    }

    public <T> T process(
            EventEnvelope<?> event,
            String eventType,
            Supplier<T> action
    ) {
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
        }

        deadLetterEventService.saveFailure(
                event.getEventId(),
                eventType,
                serializeEvent(event),
                lastFailure.getMessage(),
                MAX_RETRY_ATTEMPTS
        );
        throw lastFailure;
    }

    private String serializeEvent(EventEnvelope<?> event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            return String.valueOf(event);
        }
    }
}
