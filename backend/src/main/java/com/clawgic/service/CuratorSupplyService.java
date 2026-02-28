package com.clawgic.service;

import com.clawgic.model.Round;
import com.clawgic.repository.CuratorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CuratorSupplyService {

    private static final Logger log = LoggerFactory.getLogger(CuratorSupplyService.class);
    private static final int SUPPLY_RATIO_SCALE = 4;

    @Value("${clawgic.curation.target-reveals-per-pair:3}")
    private int targetRevealsPerPair;

    @Value("${clawgic.curation.expected-reveals-per-curator:6}")
    private int expectedRevealsPerCurator;

    private final CuratorRepository curatorRepository;

    public CuratorSupplyService(CuratorRepository curatorRepository) {
        this.curatorRepository = curatorRepository;
    }

    public CuratorSupplySnapshot computeForRound(Round round) {
        if (round == null || round.getMarket() == null || round.getMarket().getId() == null) {
            throw new IllegalArgumentException("round market context is required");
        }

        int generatedPairs = round.getPairs() != null ? Math.max(0, round.getPairs()) : 0;
        Integer marketId = round.getMarket().getId();
        int activeCurators = safeCount(curatorRepository.countByMarketId(marketId));
        int effectiveTargetRevealsPerPair = effectiveOrDefault(targetRevealsPerPair);
        int effectiveExpectedRevealsPerCurator = effectiveOrDefault(expectedRevealsPerCurator);
        int requiredCurators = calculateRequiredCurators(
                generatedPairs,
                effectiveTargetRevealsPerPair,
                effectiveExpectedRevealsPerCurator);
        BigDecimal supplyRatio = calculateSupplyRatio(activeCurators, requiredCurators);

        CuratorSupplySnapshot snapshot = new CuratorSupplySnapshot(
                generatedPairs,
                effectiveTargetRevealsPerPair,
                effectiveExpectedRevealsPerCurator,
                requiredCurators,
                activeCurators,
                supplyRatio
        );

        log.info(
                "Curator supply snapshot computed: roundId={}, marketId={}, pairs={}, targetRevealsPerPair={}, "
                        + "expectedRevealsPerCurator={}, requiredCurators={}, activeCurators={}, supplyRatio={}",
                round.getId(),
                marketId,
                snapshot.generatedPairs(),
                snapshot.targetRevealsPerPair(),
                snapshot.expectedRevealsPerCurator(),
                snapshot.requiredCurators(),
                snapshot.activeCurators(),
                snapshot.supplyRatio()
        );

        return snapshot;
    }

    private static int calculateRequiredCurators(
            int generatedPairs,
            int targetRevealsPerPair,
            int expectedRevealsPerCurator) {
        long requiredReveals = (long) generatedPairs * targetRevealsPerPair;
        if (requiredReveals <= 0L) {
            return 0;
        }

        return (int) ((requiredReveals + expectedRevealsPerCurator - 1L) / expectedRevealsPerCurator);
    }

    private static BigDecimal calculateSupplyRatio(int activeCurators, int requiredCurators) {
        if (requiredCurators == 0) {
            return BigDecimal.ONE.setScale(SUPPLY_RATIO_SCALE, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(activeCurators)
                .divide(BigDecimal.valueOf(requiredCurators), SUPPLY_RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private static int safeCount(long count) {
        if (count <= 0L) {
            return 0;
        }
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    private static int effectiveOrDefault(int configuredValue) {
        return configuredValue > 0 ? configuredValue : 1;
    }

    public record CuratorSupplySnapshot(
            int generatedPairs,
            int targetRevealsPerPair,
            int expectedRevealsPerCurator,
            int requiredCurators,
            int activeCurators,
            BigDecimal supplyRatio
    ) {
    }
}
