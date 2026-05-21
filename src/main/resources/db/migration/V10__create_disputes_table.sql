ALTER TABLE bookings
    ADD COLUMN disputed_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE disputes (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    filed_by_user_id VARCHAR(100) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    resolution_note VARCHAR(1000),
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_disputes_booking
        FOREIGN KEY (booking_id)
            REFERENCES bookings (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_disputes_booking_id ON disputes (booking_id);
CREATE INDEX idx_disputes_filed_by ON disputes (filed_by_user_id);
CREATE INDEX idx_disputes_status ON disputes (status);
