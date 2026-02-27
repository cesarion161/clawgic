package com.moltrank.clawgic.service;

import com.moltrank.clawgic.config.ClawgicRuntimeProperties;
import com.moltrank.clawgic.model.ClawgicMatch;
import com.moltrank.clawgic.model.ClawgicMatchStatus;
import com.moltrank.clawgic.model.ClawgicStakingLedger;
import com.moltrank.clawgic.model.ClawgicTournament;
import com.moltrank.clawgic.model.ClawgicTournamentEntry;
import com.moltrank.clawgic.model.ClawgicTournamentEntryStatus;
import com.moltrank.clawgic.model.ClawgicTournamentStatus;
import com.moltrank.clawgic.repository.ClawgicMatchRepository;
import com.moltrank.clawgic.repository.ClawgicStakingLedgerRepository;
import com.moltrank.clawgic.repository.ClawgicTournamentEntryRepository;
import com.moltrank.clawgic.repository.ClawgicTournamentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClawgicSettlementAccountingService {

    private static final Logger log = LoggerFactory.getLogger(ClawgicSettlementAccountingService.class);

    private final ClawgicTournamentRepository clawgicTournamentRepository;
    private final ClawgicMatchRepository clawgicMatchRepository;
    private final ClawgicStakingLedgerRepository clawgicStakingLedgerRepository;
    private final ClawgicTournamentEntryRepository clawgicTournamentEntryRepository;
    private final ClawgicRuntimeProperties clawgicRuntimeProperties;

    @Transactional
    public SettlementResult settleTournamentIfCompleted(UUID tournamentId, OffsetDateTime now) {
        ClawgicTournament tournament = clawgicTournamentRepository.findByTournamentIdForUpdate(tournamentId).orElse(null);
        if (tournament == null) {
            return SettlementResult.notApplied(tournamentId, "TOURNAMENT_NOT_FOUND");
        }
        if (tournament.getStatus() != ClawgicTournamentStatus.COMPLETED) {
            return SettlementResult.notApplied(tournamentId, "TOURNAMENT_NOT_COMPLETED");
        }

        List<ClawgicStakingLedger> ledgers =
                clawgicStakingLedgerRepository.findByTournamentIdOrderByCreatedAtAscForUpdate(tournamentId);
        if (ledgers.isEmpty()) {
            log.warn("Skipping settlement for completed tournament {} because no ledger rows exist", tournamentId);
            return SettlementResult.notApplied(tournamentId, "NO_LEDGER_ROWS");
        }
        if (allLedgersSettled(ledgers)) {
            return SettlementResult.alreadySettled(tournamentId);
        }
        if (anyLedgerSettled(ledgers)) {
            throw new IllegalStateException(
                    "Cannot settle tournament " + tournamentId + " because ledger rows are partially settled"
            );
        }
        if (tournament.getWinnerAgentId() == null) {
            throw new IllegalStateException(
                    "Cannot settle tournament " + tournamentId + " because winner_agent_id is missing"
            );
        }

        List<ClawgicMatch> matches =
                clawgicMatchRepository.findByTournamentIdOrderByBracketRoundAscBracketPositionAscCreatedAtAsc(tournamentId);
        Set<UUID> forfeitedAgentIds = resolveForfeitedAgentIds(matches, tournamentId);

        BigDecimal requestedJudgeFeeTotal = resolveRequestedJudgeFeeTotal(tournament);
        BigDecimal systemRetentionRate = resolveSystemRetentionRate();

        ClawgicSettlementMath.SettlementPlan settlementPlan = ClawgicSettlementMath.planSettlement(
                ledgers,
                tournament.getWinnerAgentId(),
                forfeitedAgentIds,
                requestedJudgeFeeTotal,
                systemRetentionRate
        );

        Map<UUID, ClawgicSettlementMath.LedgerAllocation> allocationsByStakeId = toAllocationMap(
                settlementPlan.allocations()
        );
        Map<UUID, ClawgicTournamentEntry> entriesByAgentId = toEntryMap(
                clawgicTournamentEntryRepository.findByTournamentIdOrderByCreatedAtAsc(tournamentId)
        );

        OffsetDateTime lockTimestamp = tournament.getStartedAt() == null ? now : tournament.getStartedAt();
        List<ClawgicTournamentEntry> entriesToUpdate = new ArrayList<>();
        for (ClawgicStakingLedger ledger : ledgers) {
            ClawgicSettlementMath.LedgerAllocation allocation = allocationsByStakeId.get(ledger.getStakeId());
            if (allocation == null) {
                throw new IllegalStateException(
                        "Missing settlement allocation for stake " + ledger.getStakeId() + " in tournament " + tournamentId
                );
            }

            ledger.setJudgeFeeDeducted(allocation.judgeFeeDeducted());
            ledger.setSystemRetention(allocation.systemRetention());
            ledger.setRewardPayout(allocation.rewardPayout());
            ledger.setStatus(allocation.status());
            ledger.setLockedAt(ledger.getLockedAt() == null ? lockTimestamp : ledger.getLockedAt());
            ledger.setSettledAt(now);
            ledger.setUpdatedAt(now);
            if (allocation.forfeited() && ledger.getForfeitedAt() == null) {
                ledger.setForfeitedAt(now);
            }
            ledger.setSettlementNote(buildSettlementNote(allocation, settlementPlan));

            if (allocation.forfeited()) {
                ClawgicTournamentEntry entry = entriesByAgentId.get(allocation.agentId());
                if (entry != null && entry.getStatus() != ClawgicTournamentEntryStatus.FORFEITED) {
                    entry.setStatus(ClawgicTournamentEntryStatus.FORFEITED);
                    entry.setUpdatedAt(now);
                    entriesToUpdate.add(entry);
                }
            }
        }

        clawgicStakingLedgerRepository.saveAll(ledgers);
        if (!entriesToUpdate.isEmpty()) {
            clawgicTournamentEntryRepository.saveAll(entriesToUpdate);
        }

        return SettlementResult.applied(
                tournamentId,
                settlementPlan.totalPool(),
                settlementPlan.judgeFeeTotal(),
                settlementPlan.systemRetentionTotal(),
                settlementPlan.rewardPool()
        );
    }

    private static Map<UUID, ClawgicSettlementMath.LedgerAllocation> toAllocationMap(
            List<ClawgicSettlementMath.LedgerAllocation> allocations
    ) {
        Map<UUID, ClawgicSettlementMath.LedgerAllocation> allocationsByStakeId = new HashMap<>();
        for (ClawgicSettlementMath.LedgerAllocation allocation : allocations) {
            allocationsByStakeId.put(allocation.stakeId(), allocation);
        }
        return allocationsByStakeId;
    }

    private static Map<UUID, ClawgicTournamentEntry> toEntryMap(List<ClawgicTournamentEntry> entries) {
        Map<UUID, ClawgicTournamentEntry> byAgentId = new HashMap<>();
        for (ClawgicTournamentEntry entry : entries) {
            byAgentId.put(entry.getAgentId(), entry);
        }
        return byAgentId;
    }

    private BigDecimal resolveRequestedJudgeFeeTotal(ClawgicTournament tournament) {
        BigDecimal feePerCompletedMatch = clawgicRuntimeProperties.getTournament().getJudgeFeeUsdcPerCompletedMatch();
        if (feePerCompletedMatch == null || feePerCompletedMatch.signum() < 0) {
            throw new IllegalStateException(
                    "clawgic.tournament.judge-fee-usdc-per-completed-match must be non-negative"
            );
        }
        int matchesCompleted = tournament.getMatchesCompleted() == null ? 0 : Math.max(0, tournament.getMatchesCompleted());
        return feePerCompletedMatch.multiply(BigDecimal.valueOf(matchesCompleted));
    }

    private BigDecimal resolveSystemRetentionRate() {
        BigDecimal rate = clawgicRuntimeProperties.getTournament().getSystemRetentionRate();
        if (rate == null) {
            throw new IllegalStateException("clawgic.tournament.system-retention-rate must be configured");
        }
        return rate.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private static Set<UUID> resolveForfeitedAgentIds(List<ClawgicMatch> matches, UUID tournamentId) {
        Set<UUID> forfeitedAgentIds = new HashSet<>();
        for (ClawgicMatch match : matches) {
            if (match.getStatus() != ClawgicMatchStatus.FORFEITED) {
                continue;
            }
            UUID forfeitedAgentId = resolveForfeitedAgentId(match);
            if (forfeitedAgentId == null) {
                throw new IllegalStateException(
                        "Cannot settle tournament " + tournamentId
                                + " because forfeited match " + match.getMatchId()
                                + " does not identify a forfeited agent"
                );
            }
            forfeitedAgentIds.add(forfeitedAgentId);
        }
        return Set.copyOf(forfeitedAgentIds);
    }

    private static UUID resolveForfeitedAgentId(ClawgicMatch match) {
        UUID agent1Id = match.getAgent1Id();
        UUID agent2Id = match.getAgent2Id();
        UUID winnerAgentId = match.getWinnerAgentId();
        if (agent1Id == null || agent2Id == null || winnerAgentId == null) {
            return null;
        }
        if (winnerAgentId.equals(agent1Id)) {
            return agent2Id;
        }
        if (winnerAgentId.equals(agent2Id)) {
            return agent1Id;
        }
        return null;
    }

    private static boolean allLedgersSettled(List<ClawgicStakingLedger> ledgers) {
        return ledgers.stream().allMatch(ledger -> ledger.getSettledAt() != null);
    }

    private static boolean anyLedgerSettled(List<ClawgicStakingLedger> ledgers) {
        return ledgers.stream().anyMatch(ledger -> ledger.getSettledAt() != null);
    }

    private static String buildSettlementNote(
            ClawgicSettlementMath.LedgerAllocation allocation,
            ClawgicSettlementMath.SettlementPlan settlementPlan
    ) {
        String outcome = allocation.forfeited()
                ? "FORFEITED"
                : allocation.winner() ? "WINNER" : "SETTLED_NO_PAYOUT";
        return "C30_" + outcome
                + " pool=" + settlementPlan.totalPool()
                + " judge_fee=" + settlementPlan.judgeFeeTotal()
                + " system_retention=" + settlementPlan.systemRetentionTotal()
                + " reward_pool=" + settlementPlan.rewardPool();
    }

    public record SettlementResult(
            UUID tournamentId,
            boolean applied,
            boolean alreadySettled,
            String reason,
            BigDecimal totalPool,
            BigDecimal totalJudgeFee,
            BigDecimal totalSystemRetention,
            BigDecimal totalRewardPayout
    ) {
        static SettlementResult applied(
                UUID tournamentId,
                BigDecimal totalPool,
                BigDecimal totalJudgeFee,
                BigDecimal totalSystemRetention,
                BigDecimal totalRewardPayout
        ) {
            return new SettlementResult(
                    tournamentId,
                    true,
                    false,
                    "APPLIED",
                    totalPool,
                    totalJudgeFee,
                    totalSystemRetention,
                    totalRewardPayout
            );
        }

        static SettlementResult alreadySettled(UUID tournamentId) {
            return new SettlementResult(
                    tournamentId,
                    false,
                    true,
                    "ALREADY_SETTLED",
                    null,
                    null,
                    null,
                    null
            );
        }

        static SettlementResult notApplied(UUID tournamentId, String reason) {
            return new SettlementResult(
                    tournamentId,
                    false,
                    false,
                    reason,
                    null,
                    null,
                    null,
                    null
            );
        }
    }
}
