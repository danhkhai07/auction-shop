--tạo cột users
CREATE TABLE IF NOT EXISTS users (
    numeric_id INTEGER GENERATED ALWAYS AS IDENTITY UNIQUE,
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_name VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_name),
    CONSTRAINT chk_user_roles_role_name CHECK (role_name IN ('GUEST', 'USER', 'ADMIN'))
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);

CREATE TABLE IF NOT EXISTS auctions (
    numeric_id INTEGER GENERATED ALWAYS AS IDENTITY UNIQUE,
    id VARCHAR(64) PRIMARY KEY,
    seller_id VARCHAR(64) REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    title VARCHAR(255),
    description TEXT,
    start_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    current_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    starts_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_auctions_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ENDED', 'CANCELLED')),
    CONSTRAINT chk_auctions_price_non_negative CHECK (start_price >= 0 AND current_price >= 0),
    CONSTRAINT chk_auctions_end_after_start CHECK (ends_at IS NULL OR ends_at >= starts_at)
);

CREATE INDEX IF NOT EXISTS idx_auctions_status_ends_at ON auctions(status, ends_at);
CREATE INDEX IF NOT EXISTS idx_auctions_seller_id ON auctions(seller_id);

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
