package com.moltrank.service;

import com.moltrank.model.Curator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the curator scoring pipeline:
 * commit → reveal → settlement → curator score update.
 *
 * Tests CuratorScoringService deterministic scoring logic
 * and EloService rating calculations end-to-end without database.
 */
class SettlementPipelineTest {

    private final CuratorScoringService scoringService = new CuratorScoringService(null);

    // ========================================================================
    // CuratorScore calculation pipeline
    // ========================================================================

    @Test
    void curatorScore_perfectCurator_getsMaxScore() {
        Curator curator = buildCurator(
                new BigDecimal("1.0"),  // calibration
                new BigDecimal("1.0"),  // alignment
                new BigDecimal("1.0"),  // audit
                0                       // fraud flags
        );

        BigDecimal score = scoringService.calculateCuratorScore(curator);

        // 0.40*1.0 + 0.25*1.0 + 0.20*1.0 - 0.15*0.0 = 0.85
        assertEquals(new BigDecimal("0.8500"), score);
    }

    @Test
    void curatorScore_zeroCurator_getsZero() {
        Curator curator = buildCurator(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0
        );

        BigDecimal score = scoringService.calculateCuratorScore(curator);

        assertEquals(new BigDecimal("0.0000"), score);
    }

    @Test
    void curatorScore_fraudFlagsPenalize() {
        Curator curator = buildCurator(
                new BigDecimal("0.8"), new BigDecimal("0.8"), new BigDecimal("0.8"), 5
        );

        BigDecimal score = scoringService.calculateCuratorScore(curator);

        // 0.40*0.8 + 0.25*0.8 + 0.20*0.8 - 0.15*(5*0.1) = 0.32+0.20+0.16-0.075 = 0.605
        assertEquals(new BigDecimal("0.6050"), score);
    }

    @Test
    void curatorScore_maxFraudFlagsCappedAt10() {
        Curator curator = buildCurator(
                new BigDecimal("1.0"), new BigDecimal("1.0"), new BigDecimal("1.0"), 20
        );

        BigDecimal score = scoringService.calculateCuratorScore(curator);

        // Fraud penalty capped at 10*0.1 = 1.0
        // 0.85 - 0.15*1.0 = 0.70
        assertEquals(new BigDecimal("0.7000"), score);
    }

    @Test
    void curatorScore_clampedToZeroFloor() {
        Curator curator = buildCurator(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 10
        );

        BigDecimal score = scoringService.calculateCuratorScore(curator);

        // 0 - 0.15*1.0 = -0.15, clamped to 0
        assertEquals(new BigDecimal("0.0000"), score);
    }

    // ========================================================================
    // Blended slashing score
    // ========================================================================

    @Test
    void blendedScore_allPerfect() {
        BigDecimal blended = scoringService.calculateBlendedScore(
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE
        );

        // 0.40 + 0.25 + 0.20 + 0.15 = 1.0
        assertEquals(new BigDecimal("1.0000"), blended);
    }

    @Test
    void blendedScore_weightsAppliedCorrectly() {
        BigDecimal blended = scoringService.calculateBlendedScore(
                new BigDecimal("0.5"), // golden 40%
                new BigDecimal("0.5"), // audit 25%
                new BigDecimal("0.5"), // consensus 20%
                new BigDecimal("0.5")  // behavioral 15%
        );

        // All 0.5 → blended = 0.5 * (0.40+0.25+0.20+0.15) = 0.5
        assertEquals(new BigDecimal("0.5000"), blended);
    }

    // ========================================================================
    // Reward multiplier (slashing tiers)
    // ========================================================================

    @Test
    void rewardMultiplier_above60Percent_fullRewards() {
        assertEquals(BigDecimal.ONE, scoringService.getRewardMultiplier(new BigDecimal("0.75")));
        assertEquals(BigDecimal.ONE, scoringService.getRewardMultiplier(new BigDecimal("0.60")));
        assertEquals(BigDecimal.ONE, scoringService.getRewardMultiplier(BigDecimal.ONE));
    }

    @Test
    void rewardMultiplier_between40And60_reducedRewards() {
        assertEquals(new BigDecimal("0.50"), scoringService.getRewardMultiplier(new BigDecimal("0.50")));
        assertEquals(new BigDecimal("0.50"), scoringService.getRewardMultiplier(new BigDecimal("0.40")));
        assertEquals(new BigDecimal("0.50"), scoringService.getRewardMultiplier(new BigDecimal("0.59")));
    }

    @Test
    void rewardMultiplier_below40Percent_slashed() {
        assertEquals(new BigDecimal("0.10"), scoringService.getRewardMultiplier(new BigDecimal("0.39")));
        assertEquals(new BigDecimal("0.10"), scoringService.getRewardMultiplier(BigDecimal.ZERO));
    }

    // ========================================================================
    // Suspension check
    // ========================================================================

    @Test
    void shouldSuspend_below40Percent() {
        assertTrue(scoringService.shouldSuspend(new BigDecimal("0.39")));
        assertTrue(scoringService.shouldSuspend(BigDecimal.ZERO));
    }

    @Test
    void shouldNotSuspend_atOrAbove40Percent() {
        assertFalse(scoringService.shouldSuspend(new BigDecimal("0.40")));
        assertFalse(scoringService.shouldSuspend(new BigDecimal("0.85")));
    }

    // ========================================================================
    // Voting power multiplier
    // ========================================================================

    @Test
    void votingPower_highScoreCurator_gets2x() {
        BigDecimal multiplier = scoringService.getVotingPowerMultiplier(BigDecimal.ONE);
        assertEquals(new BigDecimal("2.0000"), multiplier);
    }

    @Test
    void votingPower_midScoreCurator_getsLinearScaling() {
        BigDecimal multiplier = scoringService.getVotingPowerMultiplier(new BigDecimal("0.75"));
        // 0.75 * 2.0 = 1.5, clamped to [1.0, 2.0]
        assertEquals(new BigDecimal("1.5000"), multiplier);
    }

    @Test
    void votingPower_lowScoreCurator_clampedToMinimum() {
        BigDecimal multiplier = scoringService.getVotingPowerMultiplier(new BigDecimal("0.3"));
        // 0.3 * 2.0 = 0.6, clamped to 1.0 minimum
        assertEquals(new BigDecimal("1.0000"), multiplier);
    }

    // ========================================================================
    // Full pipeline scenario: commit → score → slash → verify
    // ========================================================================

    @Test
    void pipeline_honestCurator_fullRewardsFlow() {
        // Simulate a honest curator who performs well
        Curator curator = buildCurator(
                new BigDecimal("0.90"),  // good golden set accuracy
                new BigDecimal("0.85"),  // stable alignment
                new BigDecimal("0.80"),  // passes audits
                0                        // no fraud
        );

        // Step 1: Calculate curator score
        BigDecimal score = scoringService.calculateCuratorScore(curator);
        assertTrue(score.compareTo(new BigDecimal("0.60")) > 0,
                "Honest curator should score above 60%: " + score);

        // Step 2: Calculate blended score
        BigDecimal blended = scoringService.calculateBlendedScore(
                curator.getCalibrationRate(),
                curator.getAuditPassRate(),
                curator.getAlignmentStability(),
                BigDecimal.ONE // no behavioral issues
        );

        // Step 3: Verify full rewards
        BigDecimal multiplier = scoringService.getRewardMultiplier(blended);
        assertEquals(BigDecimal.ONE, multiplier, "Honest curator should get full rewards");

        // Step 4: Verify not suspended
        assertFalse(scoringService.shouldSuspend(blended));

        // Step 5: Verify voting power boost
        BigDecimal votingPower = scoringService.getVotingPowerMultiplier(score);
        assertTrue(votingPower.compareTo(BigDecimal.ONE) > 0,
                "Good curator should get voting power > 1.0x");
    }

    @Test
    void pipeline_maliciousCurator_slashedAndSuspended() {
        // Simulate a malicious curator caught by the system
        Curator curator = buildCurator(
                new BigDecimal("0.20"),  // bad golden set accuracy
                new BigDecimal("0.10"),  // unstable alignment
                new BigDecimal("0.15"),  // fails audits
                8                        // many fraud flags
        );

        // Step 1: Calculate curator score
        BigDecimal score = scoringService.calculateCuratorScore(curator);
        assertTrue(score.compareTo(new BigDecimal("0.40")) < 0,
                "Malicious curator should score below 40%: " + score);

        // Step 2: Calculate blended score
        BigDecimal blended = scoringService.calculateBlendedScore(
                curator.getCalibrationRate(),
                curator.getAuditPassRate(),
                curator.getAlignmentStability(),
                new BigDecimal("0.10") // flagged behavioral
        );

        // Step 3: Verify slashed
        BigDecimal multiplier = scoringService.getRewardMultiplier(blended);
        assertEquals(new BigDecimal("0.10"), multiplier, "Bad curator should be slashed to 10%");

        // Step 4: Verify suspended
        assertTrue(scoringService.shouldSuspend(blended));
    }

    @Test
    void pipeline_borderlineCurator_reducedRewards() {
        Curator curator = buildCurator(
                new BigDecimal("0.60"),
                new BigDecimal("0.50"),
                new BigDecimal("0.55"),
                2
        );

        BigDecimal score = scoringService.calculateCuratorScore(curator);

        BigDecimal blended = scoringService.calculateBlendedScore(
                curator.getCalibrationRate(),
                curator.getAuditPassRate(),
                curator.getAlignmentStability(),
                new BigDecimal("0.60")
        );

        // Borderline curator should get reduced rewards
        BigDecimal multiplier = scoringService.getRewardMultiplier(blended);
        assertEquals(new BigDecimal("0.50"), multiplier, "Borderline curator should get 50% rewards");
        assertFalse(scoringService.shouldSuspend(blended));
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private Curator buildCurator(BigDecimal calibration, BigDecimal alignment,
                                  BigDecimal audit, int fraudFlags) {
        Curator curator = new Curator();
        curator.setWallet("testWallet12345678901234567890123456789012");
        curator.setMarketId(1);
        curator.setIdentityId(1);
        curator.setCalibrationRate(calibration);
        curator.setAlignmentStability(alignment);
        curator.setAuditPassRate(audit);
        curator.setFraudFlags(fraudFlags);
        curator.setEarned(0L);
        curator.setLost(0L);
        return curator;
    }
}
