package com.moltrank.controller.dto;

import com.moltrank.model.Pair;
import com.moltrank.model.Round;

import java.time.OffsetDateTime;

public record PairResponse(
        Integer id,
        Integer roundId,
        PostResponse postA,
        PostResponse postB,
        OffsetDateTime commitDeadline,
        OffsetDateTime revealDeadline,
        Boolean isGolden,
        Boolean isAudit,
        Long totalStake,
        Long reward,
        OffsetDateTime createdAt
) {
    public static PairResponse from(Pair pair) {
        Round round = pair.getRound();
        Integer roundId = round != null ? round.getId() : null;
        OffsetDateTime commitDeadline = round != null ? round.getCommitDeadline() : null;
        OffsetDateTime revealDeadline = round != null ? round.getRevealDeadline() : null;

        return new PairResponse(
                pair.getId(),
                roundId,
                PostResponse.from(pair.getPostA()),
                PostResponse.from(pair.getPostB()),
                commitDeadline,
                revealDeadline,
                pair.getIsGolden(),
                pair.getIsAudit(),
                pair.getTotalStake(),
                pair.getReward(),
                pair.getCreatedAt()
        );
    }
}
