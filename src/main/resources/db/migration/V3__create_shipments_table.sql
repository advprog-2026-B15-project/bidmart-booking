CREATE TABLE shipments (
                           id BIGSERIAL PRIMARY KEY,
                           booking_id BIGINT NOT NULL UNIQUE,
                           status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                           tracking_number VARCHAR(100),
                           courier_name VARCHAR(100),
                           shipped_at TIMESTAMP WITH TIME ZONE,
                           delivered_at TIMESTAMP WITH TIME ZONE,
                           created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                           updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                           CONSTRAINT fk_shipments_booking
                               FOREIGN KEY (booking_id)
                                   REFERENCES bookings (id)
                                   ON DELETE CASCADE
);

CREATE INDEX idx_shipments_status ON shipments (status);
