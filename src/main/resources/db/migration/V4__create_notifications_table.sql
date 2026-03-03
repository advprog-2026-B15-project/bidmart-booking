CREATE TABLE notifications (
                               id BIGSERIAL PRIMARY KEY,
                               user_id VARCHAR(100) NOT NULL,
                               type VARCHAR(20) NOT NULL,
                               title VARCHAR(160) NOT NULL,
                               message VARCHAR(500) NOT NULL,
                               is_read BOOLEAN NOT NULL DEFAULT FALSE,
                               read_at TIMESTAMP WITH TIME ZONE,
                               related_auction_id VARCHAR(100),
                               related_booking_id BIGINT,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                               CONSTRAINT fk_notifications_booking
                                   FOREIGN KEY (related_booking_id)
                                       REFERENCES bookings (id)
                                       ON DELETE SET NULL
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications (created_at);
