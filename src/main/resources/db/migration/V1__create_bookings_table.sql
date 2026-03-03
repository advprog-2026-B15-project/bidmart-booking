CREATE TABLE bookings (
                          id BIGSERIAL PRIMARY KEY,
                          source_event_id VARCHAR(100) UNIQUE,
                          auction_id VARCHAR(100) NOT NULL UNIQUE,
                          listing_id VARCHAR(100) NOT NULL,
                          seller_user_id VARCHAR(100) NOT NULL,
                          buyer_user_id VARCHAR(100) NOT NULL,
                          status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
                          total_amount BIGINT NOT NULL CHECK (total_amount >= 0),
                          currency VARCHAR(3) NOT NULL DEFAULT 'IDR',
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bookings_buyer_user_id ON bookings (buyer_user_id);
CREATE INDEX idx_bookings_status ON bookings (status);
