CREATE TABLE click_event (
        id            BIGSERIAL       PRIMARY KEY,
        short_url_id  BIGINT          NOT NULL REFERENCES shorturl(id) ON DELETE CASCADE,
        clicked_at    TIMESTAMP       NOT NULL DEFAULT now(),
        referrer      VARCHAR(512),
        user_agent    VARCHAR(512),
        ip_hash       VARCHAR(64)     -- SHA-256 hex = 64 chars
);

CREATE INDEX idx_click_event_short_url_id ON click_event (short_url_id);
CREATE INDEX idx_click_event_clicked_at ON click_event (short_url_id, clicked_at);