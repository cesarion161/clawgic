package com.moltrank.controller.dto;

import com.moltrank.model.Round;
import com.moltrank.model.RoundStatus;
import com.moltrank.service.CuratorSupplyService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ActiveRoundResponse(
        Integer id,
        Integer roundId,
        RoundStatus status,
        OffsetDateTime commitDeadline,
        OffsetDateTime revealDeadline,
        Integer totalPairs,
        Integer remainingPairs,
        Integer targetRevealsPerPair,
        Integer expectedRevealsPerCurator,
        Integer requiredCurators,
        Integer activeCurators,
        BigDecimal supplyRatio
) {
    public static ActiveRoundResponse from(
            Round round,
            Integer totalPairs,
            Integer remainingPairs,
            CuratorSupplyService.CuratorSupplySnapshot supplySnapshot) {
        return new ActiveRoundResponse(
                round.getId(),
                round.getId(),
                round.getStatus(),
                round.getCommitDeadline(),
                round.getRevealDeadline(),
                totalPairs,
                remainingPairs,
                supplySnapshot.targetRevealsPerPair(),
                supplySnapshot.expectedRevealsPerCurator(),
                supplySnapshot.requiredCurators(),
                supplySnapshot.activeCurators(),
                supplySnapshot.supplyRatio()
        );
    }
}
