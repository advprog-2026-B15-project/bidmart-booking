package com.example.bidmartbooking.booking.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventEnvelope<T> {
    private String eventId;
    private String eventType;
    private Integer eventVersion;
    private String occurredAt;
    private String source;
    private T payload;
}
