package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.model.ProcessedEvent;
import com.example.bidmartbooking.booking.repository.ProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    public ProcessedEventService(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional(readOnly = true)
    public boolean hasProcessed(String eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    @Transactional
    public ProcessedEvent markProcessed(String eventId, String eventType) {
        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(eventId);
        processedEvent.setEventType(eventType);
        return processedEventRepository.save(processedEvent);
    }
}
