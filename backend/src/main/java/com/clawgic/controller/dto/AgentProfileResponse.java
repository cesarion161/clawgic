package com.clawgic.controller.dto;

import java.util.List;

public record AgentProfileResponse(
        String agentId,
        int totalPosts,
        int totalMatchups,
        int totalWins,
        int avgElo,
        int maxElo,
        double winRate,
        List<PostResponse> posts
) {
}
