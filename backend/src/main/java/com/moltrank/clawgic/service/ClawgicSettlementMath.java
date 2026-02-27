package com.moltrank.clawgic.service;

import com.moltrank.clawgic.model.ClawgicStakingLedger;
import com.moltrank.clawgic.model.ClawgicStakingLedgerStatus;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@NoArgsConstructor
final class ClawgicSettlementMath {

    static final int USDC_SCALE = 6;
    private static final BigDecimal ZERO_USDC = BigDecimal.ZERO.setScale(USDC_SCALE, RoundingMode.HALF_UP);
    private static final BigDecimal ONE = BigDecimal.ONE;

    static SettlementPlan planSettlement(
            List<ClawgicStakingLedger> ledgers,
            UUID winnerAgentId,
            Set<UUID> forfeitedAgentIds,
            BigDecimal requestedJudgeFeeTotal,
            BigDecimal systemRetentionRate
    ) {
        Objects.requireNonNull(ledgers, "ledgers must not be null");
        Objects.requireNonNull(winnerAgentId, "winnerAgentId must not be null");
        Objects.requireNonNull(forfeitedAgentIds, "forfeitedAgentIds must not be null");
        Objects.requireNonNull(requestedJudgeFeeTotal, "requestedJudgeFeeTotal must not be null");
        Objects.requireNonNull(systemRetentionRate, "systemRetentionRate must not be null");

        if (ledgers.isEmpty()) {
            throw new IllegalStateException("Cannot settle tournament without staking ledger rows");
        }

        BigDecimal normalizedRetentionRate = normalizeRetentionRate(systemRetentionRate);
        BigDecimal totalPool = sumStakedAmount(ledgers);
        BigDecimal judgeFeeTotal = min(totalPool, scaleNonNegative(requestedJudgeFeeTotal));
        BigDecimal rewardPoolBeforeRetention = totalPool.subtract(judgeFeeTotal);
        BigDecimal systemRetentionTotal = min(
                rewardPoolBeforeRetention,
                scaleUsdc(rewardPoolBeforeRetention.multiply(normalizedRetentionRate))
        );
        BigDecimal rewardPool = rewardPoolBeforeRetention.subtract(systemRetentionTotal);

        List<ClawgicStakingLedger> winnerLedgers = ledgers.stream()
                .filter(ledger -> winnerAgentId.equals(ledger.getAgentId()))
                .toList();
        if (winnerLedgers.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot settle tournament: winner " + winnerAgentId + " has no staking ledger row"
            );
        }

        Map<UUID, BigDecimal> judgeFeeByStakeId = allocateProRata(judgeFeeTotal, ledgers);
        Map<UUID, BigDecimal> systemRetentionByStakeId = allocateProRata(systemRetentionTotal, ledgers);
        Map<UUID, BigDecimal> rewardPayoutByStakeId = allocateEvenly(rewardPool, winnerLedgers);

        List<LedgerAllocation> allocations = new ArrayList<>(ledgers.size());
        for (ClawgicStakingLedger ledger : ledgers) {
            UUID stakeId = ledger.getStakeId();
            UUID agentId = ledger.getAgentId();
            BigDecimal judgeFee = judgeFeeByStakeId.getOrDefault(stakeId, ZERO_USDC);
            BigDecimal retention = systemRetentionByStakeId.getOrDefault(stakeId, ZERO_USDC);
            BigDecimal payout = rewardPayoutByStakeId.getOrDefault(stakeId, ZERO_USDC);
            boolean winner = winnerAgentId.equals(agentId);
            boolean forfeited = forfeitedAgentIds.contains(agentId);

            allocations.add(new LedgerAllocation(
                    stakeId,
                    agentId,
                    judgeFee,
                    retention,
                    payout,
                    forfeited ? ClawgicStakingLedgerStatus.FORFEITED : ClawgicStakingLedgerStatus.SETTLED,
                    winner,
                    forfeited
            ));
        }

        return new SettlementPlan(totalPool, judgeFeeTotal, systemRetentionTotal, rewardPool, allocations);
    }

    private static BigDecimal sumStakedAmount(List<ClawgicStakingLedger> ledgers) {
        BigDecimal total = ZERO_USDC;
        for (ClawgicStakingLedger ledger : ledgers) {
            total = total.add(scaleNonNegative(ledger.getAmountStaked()));
        }
        return scaleUsdc(total);
    }

    private static Map<UUID, BigDecimal> allocateProRata(BigDecimal total, List<ClawgicStakingLedger> ledgers) {
        if (ledgers.isEmpty()) {
            return Map.of();
        }
        BigDecimal normalizedTotal = scaleNonNegative(total);
        if (normalizedTotal.signum() == 0) {
            return zeroAllocations(ledgers);
        }

        BigDecimal totalStake = ZERO_USDC;
        List<BigDecimal> weights = new ArrayList<>(ledgers.size());
        for (ClawgicStakingLedger ledger : ledgers) {
            BigDecimal stake = scaleNonNegative(ledger.getAmountStaked());
            weights.add(stake);
            totalStake = totalStake.add(stake);
        }
        if (totalStake.signum() == 0) {
            weights = new ArrayList<>(ledgers.size());
            for (int i = 0; i < ledgers.size(); i++) {
                weights.add(ONE);
            }
            totalStake = BigDecimal.valueOf(ledgers.size());
        }
        return allocateByWeights(normalizedTotal, ledgers, weights, totalStake);
    }

    private static Map<UUID, BigDecimal> allocateEvenly(BigDecimal total, List<ClawgicStakingLedger> ledgers) {
        if (ledgers.isEmpty()) {
            return Map.of();
        }
        BigDecimal normalizedTotal = scaleNonNegative(total);
        if (normalizedTotal.signum() == 0) {
            return zeroAllocations(ledgers);
        }
        List<BigDecimal> weights = new ArrayList<>(ledgers.size());
        for (int i = 0; i < ledgers.size(); i++) {
            weights.add(ONE);
        }
        return allocateByWeights(normalizedTotal, ledgers, weights, BigDecimal.valueOf(ledgers.size()));
    }

    private static Map<UUID, BigDecimal> allocateByWeights(
            BigDecimal total,
            List<ClawgicStakingLedger> ledgers,
            List<BigDecimal> weights,
            BigDecimal totalWeight
    ) {
        LinkedHashMap<UUID, BigDecimal> allocations = new LinkedHashMap<>();
        if (ledgers.size() == 1) {
            allocations.put(ledgers.getFirst().getStakeId(), total);
            return allocations;
        }

        BigDecimal remaining = total;
        int lastIndex = ledgers.size() - 1;
        for (int i = 0; i < lastIndex; i++) {
            BigDecimal weight = weights.get(i);
            BigDecimal share = scaleUsdc(total.multiply(weight).divide(totalWeight, USDC_SCALE, RoundingMode.HALF_UP));
            share = min(share, remaining);
            allocations.put(ledgers.get(i).getStakeId(), share);
            remaining = remaining.subtract(share);
        }
        allocations.put(ledgers.get(lastIndex).getStakeId(), scaleUsdc(remaining));
        return allocations;
    }

    private static Map<UUID, BigDecimal> zeroAllocations(List<ClawgicStakingLedger> ledgers) {
        LinkedHashMap<UUID, BigDecimal> allocations = new LinkedHashMap<>();
        for (ClawgicStakingLedger ledger : ledgers) {
            allocations.put(ledger.getStakeId(), ZERO_USDC);
        }
        return allocations;
    }

    private static BigDecimal normalizeRetentionRate(BigDecimal value) {
        BigDecimal normalized = value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        return normalized.setScale(USDC_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal min(BigDecimal first, BigDecimal second) {
        return first.compareTo(second) <= 0 ? first : second;
    }

    private static BigDecimal scaleNonNegative(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return ZERO_USDC;
        }
        return scaleUsdc(value);
    }

    private static BigDecimal scaleUsdc(BigDecimal value) {
        return value.setScale(USDC_SCALE, RoundingMode.HALF_UP);
    }

    record SettlementPlan(
            BigDecimal totalPool,
            BigDecimal judgeFeeTotal,
            BigDecimal systemRetentionTotal,
            BigDecimal rewardPool,
            List<LedgerAllocation> allocations
    ) {
    }

    record LedgerAllocation(
            UUID stakeId,
            UUID agentId,
            BigDecimal judgeFeeDeducted,
            BigDecimal systemRetention,
            BigDecimal rewardPayout,
            ClawgicStakingLedgerStatus status,
            boolean winner,
            boolean forfeited
    ) {
    }
}
