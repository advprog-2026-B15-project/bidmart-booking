package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.event.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReliableEventProcessorTest {

    @Mock
    private DeadLetterEventService deadLetterEventService;

    @Test
    void shouldReturnActionResultWithoutRetry() {
        ReliableEventProcessor processor = new ReliableEventProcessor(
                deadLetterEventService,
                new ObjectMapper()
        );

        String result = processor.process(buildEvent(), "BidPlaced", () -> "ok");

        assertEquals("ok", result);
        verify(deadLetterEventService, never()).saveFailure(
                any(), any(), any(), any(), any()
        );
    }

    @Test
    void shouldRetryUntilActionSucceeds() {
        ReliableEventProcessor processor = new ReliableEventProcessor(
                deadLetterEventService,
                new ObjectMapper()
        );
        AtomicInteger attempts = new AtomicInteger();

        String result = processor.process(buildEvent(), "BidPlaced", () -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IllegalStateException("temporary failure");
            }
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(3, attempts.get());
        verify(deadLetterEventService, never()).saveFailure(
                any(), any(), any(), any(), any()
        );
    }

    @Test
    void shouldPersistDeadLetterAfterRetriesExhausted() {
        ReliableEventProcessor processor = new ReliableEventProcessor(
                deadLetterEventService,
                new ObjectMapper()
        );
        IllegalStateException failure = new IllegalStateException("permanent failure");

        IllegalStateException result = assertThrows(
                IllegalStateException.class,
                () -> processor.process(buildEvent(), "BalanceReleased", () -> {
                    throw failure;
                })
        );

        assertSame(failure, result);
        verify(deadLetterEventService).saveFailure(
                eq("evt-retry-1"),
                eq("BalanceReleased"),
                any(),
                eq("permanent failure"),
                eq(3)
        );
    }

    @Test
    void shouldFallbackPayloadWhenSerializationFails() throws JsonProcessingException {
        ObjectMapper objectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("cannot serialize") {
                });
        ReliableEventProcessor processor = new ReliableEventProcessor(
                deadLetterEventService,
                objectMapper
        );

        assertThrows(
                IllegalStateException.class,
                () -> processor.process(buildEvent(), "BidPlaced", () -> {
                    throw new IllegalStateException("failed");
                })
        );

        verify(deadLetterEventService).saveFailure(
                eq("evt-retry-1"),
                eq("BidPlaced"),
                any(),
                eq("failed"),
                eq(3)
        );
    }

    private EventEnvelope<String> buildEvent() {
        EventEnvelope<String> event = new EventEnvelope<>();
        event.setEventId("evt-retry-1");
        event.setPayload("payload");
        return event;
    }
}
