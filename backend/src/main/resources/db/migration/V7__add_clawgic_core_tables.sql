-- Clawgic MVP core schema (Step C10)
-- Adds initial user/agent/Elo tables for the Clawgic domain while keeping
-- legacy MoltRank tables intact.

CREATE TABLE clawgic_users (
    wallet_address VARCHAR(128) PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE clawgic_agents (
    agent_id UUID PRIMARY KEY,
    wallet_address VARCHAR(128) NOT NULL REFERENCES clawgic_users(wallet_address) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    avatar_url TEXT,
    system_prompt TEXT NOT NULL,
    skills_markdown TEXT,
    persona TEXT,
    agents_md_source TEXT,
    api_key_encrypted TEXT NOT NULL,
    api_key_encryption_key_id VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_clawgic_agents_name_not_blank CHECK (btrim(name) <> '')
);

CREATE INDEX idx_clawgic_agents_wallet_address ON clawgic_agents(wallet_address);
CREATE INDEX idx_clawgic_agents_created_at ON clawgic_agents(created_at);

CREATE TABLE clawgic_agent_elo (
    agent_id UUID PRIMARY KEY REFERENCES clawgic_agents(agent_id) ON DELETE CASCADE,
    current_elo INTEGER NOT NULL DEFAULT 1000,
    matches_played INTEGER NOT NULL DEFAULT 0,
    matches_won INTEGER NOT NULL DEFAULT 0,
    matches_forfeited INTEGER NOT NULL DEFAULT 0,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_clawgic_agent_elo_non_negative CHECK (
        current_elo >= 0
        AND matches_played >= 0
        AND matches_won >= 0
        AND matches_forfeited >= 0
    ),
    CONSTRAINT chk_clawgic_agent_elo_won_le_played CHECK (matches_won <= matches_played),
    CONSTRAINT chk_clawgic_agent_elo_forfeit_le_played CHECK (matches_forfeited <= matches_played)
);

CREATE INDEX idx_clawgic_agent_elo_current_elo ON clawgic_agent_elo(current_elo DESC);

