CREATE TABLE booking_items (
                               id BIGSERIAL PRIMARY KEY,
                               booking_id BIGINT NOT NULL,
                               listing_id VARCHAR(100) NOT NULL,
                               item_name VARCHAR(150) NOT NULL,
                               quantity INTEGER NOT NULL CHECK (quantity > 0),
                               unit_price BIGINT NOT NULL CHECK (unit_price >= 0),
                               subtotal_amount BIGINT NOT NULL CHECK (subtotal_amount >= 0),
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                               CONSTRAINT fk_booking_items_booking
                                   FOREIGN KEY (booking_id)
                                       REFERENCES bookings (id)
                                       ON DELETE CASCADE
);

CREATE INDEX idx_booking_items_booking_id ON booking_items (booking_id);
