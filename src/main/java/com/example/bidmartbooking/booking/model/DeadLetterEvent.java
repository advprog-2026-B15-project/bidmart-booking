package com.example.bidmartbooking.booking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "dead_letter_events")
public class DeadLetterEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(name = "error_message", nullable = false, length = 1000)
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "failed_at", nullable = false, updatable = false)
    private OffsetDateTime failedAt;

    @PrePersist
    public void prePersist() {
        if (retryCount == null) {
            retryCount = 0;
        }
        if (failedAt == null) {
            failedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
