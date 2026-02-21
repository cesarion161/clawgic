ALTER TABLE commitment
    ADD COLUMN non_reveal_penalized BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN non_reveal_penalized_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_commitment_unrevealed_unpenalized
    ON commitment(revealed, non_reveal_penalized)
    WHERE revealed = false AND non_reveal_penalized = false;
