package com.moltrank.clawgic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class ClawgicAgentResponses {

    private ClawgicAgentResponses() {
    }

    public record AgentElo(
            UUID agentId,
            Integer currentElo,
            Integer matchesPlayed,
            Integer matchesWon,
            Integer matchesForfeited,
            OffsetDateTime lastUpdated
    ) {
    }

    public record AgentSummary(
            UUID agentId,
            String walletAddress,
            String name,
            String avatarUrl,
            String persona,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record AgentDetail(
            UUID agentId,
            String walletAddress,
            String name,
            String avatarUrl,
            String systemPrompt,
            String skillsMarkdown,
            String persona,
            String agentsMdSource,
            boolean apiKeyConfigured,
            AgentElo elo,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }
}
