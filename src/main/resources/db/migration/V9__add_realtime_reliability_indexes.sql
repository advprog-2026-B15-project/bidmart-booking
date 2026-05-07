CREATE INDEX idx_bookings_buyer_created_at
    ON bookings (buyer_user_id, created_at);

CREATE INDEX idx_bookings_seller_created_at
    ON bookings (seller_user_id, created_at);

CREATE INDEX idx_notifications_user_created_at
    ON notifications (user_id, created_at);

CREATE INDEX idx_notifications_user_read_created_at
    ON notifications (user_id, is_read, created_at);

CREATE INDEX idx_booking_status_audit_logs_booking_created_at
    ON booking_status_audit_logs (booking_id, created_at);

CREATE INDEX idx_dead_letter_events_type_failed_at
    ON dead_letter_events (event_type, failed_at);
