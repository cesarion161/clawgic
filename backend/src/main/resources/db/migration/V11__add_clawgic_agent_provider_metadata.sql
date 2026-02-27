-- Step C16: add provider metadata fields required by agent registration API.

ALTER TABLE clawgic_agents
    ADD COLUMN provider_type VARCHAR(32) NOT NULL DEFAULT 'OPENAI',
    ADD COLUMN provider_key_ref VARCHAR(255);

ALTER TABLE clawgic_agents
    ADD CONSTRAINT chk_clawgic_agents_provider_type
        CHECK (provider_type IN ('OPENAI', 'ANTHROPIC', 'MOCK'));
