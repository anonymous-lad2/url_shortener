CREATE TABLE shorturl (
    id          BIGSERIAL       PRIMARY KEY,
    original_url VARCHAR(2048)  NOT NULL,
    short_code  VARCHAR(11)     UNIQUE,
    created_at  TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_shorturl_short_code ON shorturl (short_code);
