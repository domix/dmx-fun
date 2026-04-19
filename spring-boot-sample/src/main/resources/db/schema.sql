CREATE TABLE IF NOT EXISTS items (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT
);

CREATE INDEX IF NOT EXISTS idx_items_name ON items (name);
