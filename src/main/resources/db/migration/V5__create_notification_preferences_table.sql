CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL UNIQUE,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_preferences_user_id
    ON notification_preferences (user_id);
