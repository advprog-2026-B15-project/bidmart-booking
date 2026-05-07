CREATE TABLE processed_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_processed_events_event_type
    ON processed_events (event_type);

CREATE TABLE dead_letter_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100),
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    error_message VARCHAR(1000) NOT NULL,
    retry_count INTEGER NOT NULL CHECK (retry_count >= 0),
    failed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dead_letter_events_event_id
    ON dead_letter_events (event_id);

CREATE INDEX idx_dead_letter_events_failed_at
    ON dead_letter_events (failed_at);
