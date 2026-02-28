package com.clawgic.clawgic.service;

import com.clawgic.clawgic.model.ClawgicStakingLedger;
import com.clawgic.clawgic.model.ClawgicStakingLedgerStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClawgicSettlementMathTest {

    @Test
    void planSettlementCalculatesPoolFeesRetentionAndWinnerPayoutWithForfeitOutcome() {
        UUID winnerAgentId = UUID.randomUUID();
        UUID agentTwo = UUID.randomUUID();
        UUID agentThree = UUID.randomUUID();
        UUID forfeitedAgentId = UUID.randomUUID();

        List<ClawgicStakingLedger> ledgers = List.of(
                ledger(winnerAgentId, "5.000000"),
                ledger(agentTwo, "5.000000"),
                ledger(agentThree, "5.000000"),
                ledger(forfeitedAgentId, "5.000000")
        );

        ClawgicSettlementMath.SettlementPlan plan = ClawgicSettlementMath.planSettlement(
                ledgers,
                winnerAgentId,
                Set.of(forfeitedAgentId),
                new BigDecimal("0.750000"),
                new BigDecimal("0.100000")
        );

        assertEquals(new BigDecimal("20.000000"), plan.totalPool());
        assertEquals(new BigDecimal("0.750000"), plan.judgeFeeTotal());
        assertEquals(new BigDecimal("1.925000"), plan.systemRetentionTotal());
        assertEquals(new BigDecimal("17.325000"), plan.rewardPool());

        Map<UUID, ClawgicSettlementMath.LedgerAllocation> byAgentId = plan.allocations().stream()
                .collect(Collectors.toMap(ClawgicSettlementMath.LedgerAllocation::agentId, Function.identity()));

        assertEquals(4, byAgentId.size());
        assertEquals(new BigDecimal("0.187500"), byAgentId.get(winnerAgentId).judgeFeeDeducted());
        assertEquals(new BigDecimal("0.481250"), byAgentId.get(winnerAgentId).systemRetention());
        assertEquals(new BigDecimal("17.325000"), byAgentId.get(winnerAgentId).rewardPayout());
        assertTrue(byAgentId.get(winnerAgentId).winner());
        assertEquals(ClawgicStakingLedgerStatus.SETTLED, byAgentId.get(winnerAgentId).status());

        assertEquals(new BigDecimal("0.000000"), byAgentId.get(agentTwo).rewardPayout());
        assertEquals(new BigDecimal("0.000000"), byAgentId.get(agentThree).rewardPayout());
        assertEquals(ClawgicStakingLedgerStatus.FORFEITED, byAgentId.get(forfeitedAgentId).status());
        assertTrue(byAgentId.get(forfeitedAgentId).forfeited());
    }

    @Test
    void planSettlementHandlesSingleEntrantWithoutDivisionErrors() {
        UUID winnerAgentId = UUID.randomUUID();
        List<ClawgicStakingLedger> ledgers = List.of(ledger(winnerAgentId, "5.000000"));

        ClawgicSettlementMath.SettlementPlan plan = ClawgicSettlementMath.planSettlement(
                ledgers,
                winnerAgentId,
                Set.of(),
                new BigDecimal("0.250000"),
                new BigDecimal("0.100000")
        );

        assertEquals(new BigDecimal("5.000000"), plan.totalPool());
        assertEquals(new BigDecimal("0.250000"), plan.judgeFeeTotal());
        assertEquals(new BigDecimal("0.475000"), plan.systemRetentionTotal());
        assertEquals(new BigDecimal("4.275000"), plan.rewardPool());
        assertEquals(1, plan.allocations().size());
        assertEquals(new BigDecimal("4.275000"), plan.allocations().getFirst().rewardPayout());
        assertEquals(ClawgicStakingLedgerStatus.SETTLED, plan.allocations().getFirst().status());
    }

    @Test
    void planSettlementRejectsInvalidWinnerThatHasNoLedgerRow() {
        List<ClawgicStakingLedger> ledgers = List.of(
                ledger(UUID.randomUUID(), "5.000000"),
                ledger(UUID.randomUUID(), "5.000000")
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> ClawgicSettlementMath.planSettlement(
                        ledgers,
                        UUID.randomUUID(),
                        Set.of(),
                        new BigDecimal("0.250000"),
                        new BigDecimal("0.050000")
                )
        );

        assertTrue(ex.getMessage().contains("has no staking ledger row"));
    }

    private static ClawgicStakingLedger ledger(UUID agentId, String amountStaked) {
        ClawgicStakingLedger ledger = new ClawgicStakingLedger();
        ledger.setStakeId(UUID.randomUUID());
        ledger.setTournamentId(UUID.randomUUID());
        ledger.setAgentId(agentId);
        ledger.setWalletAddress("0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        ledger.setAmountStaked(new BigDecimal(amountStaked));
        ledger.setStatus(ClawgicStakingLedgerStatus.ENTERED);
        ledger.setCreatedAt(OffsetDateTime.now());
        ledger.setUpdatedAt(OffsetDateTime.now());
        return ledger;
    }
}
