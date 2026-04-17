ALTER TABLE bookings
    ADD COLUMN paid_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE bookings
    ADD COLUMN completed_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_bookings_seller_user_id
    ON bookings (seller_user_id);
