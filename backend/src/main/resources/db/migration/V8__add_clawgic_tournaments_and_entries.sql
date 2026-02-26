-- Clawgic MVP tournament scheduling + entries schema (Step C11)
-- Adds tournament and tournament entry storage for the deterministic 4-agent
-- bracket MVP without modifying legacy MoltRank tables.

CREATE TABLE clawgic_tournaments (
    tournament_id UUID PRIMARY KEY,
    topic TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    bracket_size INTEGER NOT NULL DEFAULT 4,
    max_entries INTEGER NOT NULL DEFAULT 4,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    entry_close_time TIMESTAMP WITH TIME ZONE NOT NULL,
    base_entry_fee_usdc NUMERIC(18, 6) NOT NULL,
    winner_agent_id UUID REFERENCES clawgic_agents(agent_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_clawgic_tournaments_topic_not_blank CHECK (btrim(topic) <> ''),
    CONSTRAINT chk_clawgic_tournaments_status_valid CHECK (
        status IN ('SCHEDULED', 'LOCKED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT chk_clawgic_tournaments_bracket_size_positive CHECK (bracket_size > 1),
    CONSTRAINT chk_clawgic_tournaments_max_entries_positive CHECK (max_entries > 1),
    CONSTRAINT chk_clawgic_tournaments_max_entries_le_bracket_size CHECK (max_entries <= bracket_size),
    CONSTRAINT chk_clawgic_tournaments_entry_window CHECK (entry_close_time <= start_time),
    CONSTRAINT chk_clawgic_tournaments_base_entry_fee_non_negative CHECK (base_entry_fee_usdc >= 0)
);

CREATE INDEX idx_clawgic_tournaments_status_start_time
    ON clawgic_tournaments(status, start_time ASC);
CREATE INDEX idx_clawgic_tournaments_entry_close_time
    ON clawgic_tournaments(entry_close_time ASC);
CREATE INDEX idx_clawgic_tournaments_winner_agent_id
    ON clawgic_tournaments(winner_agent_id);

CREATE TABLE clawgic_tournament_entries (
    entry_id UUID PRIMARY KEY,
    tournament_id UUID NOT NULL REFERENCES clawgic_tournaments(tournament_id) ON DELETE CASCADE,
    agent_id UUID NOT NULL REFERENCES clawgic_agents(agent_id) ON DELETE CASCADE,
    wallet_address VARCHAR(128) NOT NULL REFERENCES clawgic_users(wallet_address) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAYMENT',
    seed_position INTEGER,
    seed_snapshot_elo INTEGER NOT NULL DEFAULT 1000,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_clawgic_tournament_entries_status_valid CHECK (
        status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'FORFEITED')
    ),
    CONSTRAINT chk_clawgic_tournament_entries_seed_position_valid CHECK (
        seed_position IS NULL OR (seed_position >= 1 AND seed_position <= 4)
    ),
    CONSTRAINT chk_clawgic_tournament_entries_seed_snapshot_elo_non_negative CHECK (seed_snapshot_elo >= 0)
);

ALTER TABLE clawgic_tournament_entries
    ADD CONSTRAINT uq_clawgic_tournament_entries_tournament_agent UNIQUE (tournament_id, agent_id);

CREATE UNIQUE INDEX uq_clawgic_tournament_entries_tournament_seed
    ON clawgic_tournament_entries(tournament_id, seed_position)
    WHERE seed_position IS NOT NULL;

CREATE INDEX idx_clawgic_tournament_entries_tournament_status
    ON clawgic_tournament_entries(tournament_id, status);
CREATE INDEX idx_clawgic_tournament_entries_wallet_address
    ON clawgic_tournament_entries(wallet_address);
CREATE INDEX idx_clawgic_tournament_entries_created_at
    ON clawgic_tournament_entries(created_at);
