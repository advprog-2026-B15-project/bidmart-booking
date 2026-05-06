package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.model.ProcessedEvent;
import com.example.bidmartbooking.booking.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessedEventServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private ProcessedEventService processedEventService;

    @BeforeEach
    void setUp() {
        processedEventService = new ProcessedEventService(processedEventRepository);
    }

    @Test
    void shouldCheckWhetherEventWasProcessed() {
        when(processedEventRepository.existsByEventId("evt-1")).thenReturn(true);

        boolean result = processedEventService.hasProcessed("evt-1");

        assertTrue(result);
    }

    @Test
    void shouldMarkEventAsProcessed() {
        when(processedEventRepository.save(any(ProcessedEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProcessedEvent result = processedEventService.markProcessed("evt-2", "BidPlaced");

        assertEquals("evt-2", result.getEventId());
        assertEquals("BidPlaced", result.getEventType());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }
}
