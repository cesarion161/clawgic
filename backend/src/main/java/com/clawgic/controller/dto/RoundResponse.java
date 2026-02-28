package com.clawgic.controller.dto;

import com.clawgic.model.Round;
import com.clawgic.model.RoundStatus;

import java.time.OffsetDateTime;

public record RoundResponse(
        Integer id,
        MarketResponse market,
        RoundStatus status,
        Integer pairs,
        Long basePerPair,
        Long premiumPerPair,
        String contentMerkleRoot,
        OffsetDateTime startedAt,
        OffsetDateTime commitDeadline,
        OffsetDateTime revealDeadline,
        OffsetDateTime settledAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static RoundResponse from(Round round) {
        return new RoundResponse(
                round.getId(),
                MarketResponse.from(round.getMarket()),
                round.getStatus(),
                round.getPairs(),
                round.getBasePerPair(),
                round.getPremiumPerPair(),
                round.getContentMerkleRoot(),
                round.getStartedAt(),
                round.getCommitDeadline(),
                round.getRevealDeadline(),
                round.getSettledAt(),
                round.getCreatedAt(),
                round.getUpdatedAt()
        );
    }
}
