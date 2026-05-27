--tạo cột users
CREATE TABLE IF NOT EXISTS users (
    numeric_id INTEGER GENERATED ALWAYS AS IDENTITY UNIQUE,
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    banned BOOLEAN NOT NULL DEFAULT FALSE,
    banned_reason TEXT,
    banned_at TIMESTAMPTZ,
    banned_by VARCHAR(64) REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS banned BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS banned_reason TEXT;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS banned_at TIMESTAMPTZ;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS banned_by VARCHAR(64) REFERENCES users(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_name VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_name),
    CONSTRAINT chk_user_roles_role_name CHECK (role_name IN ('GUEST', 'USER', 'ADMIN'))
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);

CREATE TABLE IF NOT EXISTS items (
    numeric_id INTEGER GENERATED ALWAYS AS IDENTITY UNIQUE,
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    seller_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_items_seller_id ON items(seller_id);

CREATE TABLE IF NOT EXISTS auctions (
    numeric_id INTEGER GENERATED ALWAYS AS IDENTITY UNIQUE,
    id VARCHAR(64) PRIMARY KEY,
    item_id VARCHAR(64) REFERENCES items(id) ON DELETE CASCADE,
    current_highest_bidder_id VARCHAR(64) REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    starting_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    current_highest_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    final_price NUMERIC(19, 2),
    start_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_auctions_status CHECK (status IN ('OPEN', 'RUNNING', 'PAUSED', 'FINISHED', 'CANCELLED')),
    CONSTRAINT chk_auctions_price_non_negative CHECK (starting_price >= 0 AND current_highest_price >= 0),
    CONSTRAINT chk_auctions_end_after_start CHECK (end_time IS NULL OR end_time >= start_time)
);

ALTER TABLE auctions
    ADD COLUMN IF NOT EXISTS item_id VARCHAR(64) REFERENCES items(id) ON DELETE CASCADE;

ALTER TABLE auctions
    ADD COLUMN IF NOT EXISTS current_highest_bidder_id VARCHAR(64) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE auctions
    ADD COLUMN IF NOT EXISTS starting_price NUMERIC(19, 2) NOT NULL DEFAULT 0;

ALTER TABLE auctions
    ADD COLUMN IF NOT EXISTS current_highest_price NUMERIC(19, 2) NOT NULL DEFAULT 0;

ALTER TABLE auctions
    ADD COLUMN IF NOT EXISTS final_price NUMERIC(19, 2);

ALTER TABLE auctions
    ADD COLUMN IF NOT EXISTS start_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE auctions
    ADD COLUMN IF NOT EXISTS end_time TIMESTAMPTZ;

UPDATE auctions
SET status = CASE status
    WHEN 'DRAFT' THEN 'OPEN'
    WHEN 'ACTIVE' THEN 'RUNNING'
    WHEN 'ENDED' THEN 'FINISHED'
    WHEN 'PAID' THEN 'FINISHED'
    ELSE status
END
WHERE status IN ('DRAFT', 'ACTIVE', 'ENDED', 'PAID');

ALTER TABLE auctions
    DROP CONSTRAINT IF EXISTS chk_auctions_status;

ALTER TABLE auctions
    ADD CONSTRAINT chk_auctions_status
        CHECK (status IN ('OPEN', 'RUNNING', 'PAUSED', 'FINISHED', 'CANCELLED', 'DRAFT', 'ACTIVE', 'ENDED'));

ALTER TABLE auctions
    DROP CONSTRAINT IF EXISTS chk_auctions_price_non_negative;

ALTER TABLE auctions
    ADD CONSTRAINT chk_auctions_price_non_negative
        CHECK (starting_price >= 0 AND current_highest_price >= 0);

ALTER TABLE auctions
    DROP CONSTRAINT IF EXISTS chk_auctions_end_after_start;

ALTER TABLE auctions
    ADD CONSTRAINT chk_auctions_end_after_start
        CHECK (end_time IS NULL OR end_time >= start_time);

CREATE INDEX IF NOT EXISTS idx_auctions_status_end_time ON auctions(status, end_time);
CREATE INDEX IF NOT EXISTS idx_auctions_item_id ON auctions(item_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_auctions_unique_item_id ON auctions(item_id);
CREATE INDEX IF NOT EXISTS idx_auctions_current_highest_bidder_id ON auctions(current_highest_bidder_id);

CREATE TABLE IF NOT EXISTS bid_transactions (
    id VARCHAR(64) PRIMARY KEY,
    auction_id VARCHAR(64) NOT NULL REFERENCES auctions(id) ON DELETE CASCADE,
    bidder_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount NUMERIC(19, 2) NOT NULL,
    placed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_bid_transactions_amount_positive CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_bid_transactions_auction_id ON bid_transactions(auction_id);
CREATE INDEX IF NOT EXISTS idx_bid_transactions_bidder_id ON bid_transactions(bidder_id);
CREATE INDEX IF NOT EXISTS idx_bid_transactions_auction_placed_at ON bid_transactions(auction_id, placed_at DESC);

CREATE TABLE IF NOT EXISTS bids (
    id VARCHAR(64) PRIMARY KEY,
    auction_id VARCHAR(64) NOT NULL REFERENCES auctions(id) ON DELETE CASCADE,
    bidder_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bid_amount NUMERIC(19, 2) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_bids_amount_positive CHECK (bid_amount > 0)
);

INSERT INTO bids (id, auction_id, bidder_id, bid_amount, timestamp)
SELECT bt.id, bt.auction_id, bt.bidder_id, bt.amount, bt.placed_at
FROM bid_transactions bt
ON CONFLICT (id) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_bids_auction_id ON bids(auction_id);
CREATE INDEX IF NOT EXISTS idx_bids_bidder_id ON bids(bidder_id);
CREATE INDEX IF NOT EXISTS idx_bids_auction_timestamp ON bids(auction_id, timestamp DESC);
