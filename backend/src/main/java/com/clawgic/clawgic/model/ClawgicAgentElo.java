package com.clawgic.clawgic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "clawgic_agent_elo")
public class ClawgicAgentElo {

    @Id
    @Column(name = "agent_id", nullable = false, updatable = false)
    private UUID agentId;

    @Column(name = "current_elo", nullable = false)
    private Integer currentElo = 1000;

    @Column(name = "matches_played", nullable = false)
    private Integer matchesPlayed = 0;

    @Column(name = "matches_won", nullable = false)
    private Integer matchesWon = 0;

    @Column(name = "matches_forfeited", nullable = false)
    private Integer matchesForfeited = 0;

    @Column(name = "last_updated", nullable = false)
    private OffsetDateTime lastUpdated = OffsetDateTime.now();
}

