CREATE TABLE commit_request_replay_guard (
    id SERIAL PRIMARY KEY,
    wallet VARCHAR(44) NOT NULL,
    request_nonce VARCHAR(64) NOT NULL,
    signed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_commit_replay_wallet_nonce UNIQUE (wallet, request_nonce)
);

CREATE INDEX idx_commit_replay_expires_at ON commit_request_replay_guard(expires_at);
