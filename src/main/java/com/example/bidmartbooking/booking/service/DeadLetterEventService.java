package com.example.bidmartbooking.booking.service;

import com.example.bidmartbooking.booking.model.DeadLetterEvent;
import com.example.bidmartbooking.booking.repository.DeadLetterEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeadLetterEventService {

    private final DeadLetterEventRepository deadLetterEventRepository;

    public DeadLetterEventService(DeadLetterEventRepository deadLetterEventRepository) {
        this.deadLetterEventRepository = deadLetterEventRepository;
    }

    @Transactional
    public DeadLetterEvent saveFailure(
            String eventId,
            String eventType,
            String payload,
            String errorMessage,
            Integer retryCount
    ) {
        DeadLetterEvent deadLetterEvent = new DeadLetterEvent();
        deadLetterEvent.setEventId(eventId);
        deadLetterEvent.setEventType(eventType);
        deadLetterEvent.setPayload(payload);
        deadLetterEvent.setErrorMessage(errorMessage);
        deadLetterEvent.setRetryCount(retryCount);
        return deadLetterEventRepository.save(deadLetterEvent);
    }
}
