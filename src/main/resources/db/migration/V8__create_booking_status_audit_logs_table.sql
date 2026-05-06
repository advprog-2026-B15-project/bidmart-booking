CREATE TABLE booking_status_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by_user_id VARCHAR(100) NOT NULL,
    changed_by_role VARCHAR(30) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_booking_status_audit_logs_booking
        FOREIGN KEY (booking_id)
            REFERENCES bookings (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_booking_status_audit_logs_booking_id
    ON booking_status_audit_logs (booking_id);

CREATE INDEX idx_booking_status_audit_logs_created_at
    ON booking_status_audit_logs (created_at);
