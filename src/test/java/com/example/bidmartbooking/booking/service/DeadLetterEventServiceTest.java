package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.model.DeadLetterEvent;
import com.example.bidmartbooking.booking.repository.DeadLetterEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeadLetterEventServiceTest {

    @Mock
    private DeadLetterEventRepository deadLetterEventRepository;

    private DeadLetterEventService deadLetterEventService;

    @BeforeEach
    void setUp() {
        deadLetterEventService = new DeadLetterEventService(deadLetterEventRepository);
    }

    @Test
    void shouldSaveFailedEvent() {
        when(deadLetterEventRepository.save(any(DeadLetterEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeadLetterEvent result = deadLetterEventService.saveFailure(
                "evt-failed",
                "BalanceReleased",
                "{\"eventId\":\"evt-failed\"}",
                "processing failed",
                3
        );

        assertEquals("evt-failed", result.getEventId());
        assertEquals("BalanceReleased", result.getEventType());
        assertEquals("processing failed", result.getErrorMessage());
        assertEquals(3, result.getRetryCount());
        verify(deadLetterEventRepository).save(any(DeadLetterEvent.class));
    }
}
