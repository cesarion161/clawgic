package com.clawgic.service;

import com.clawgic.model.Curator;
import com.clawgic.repository.CuratorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

/**
 * Curator scoring and slashing service.
 *
 * Calculates CuratorScore based on:
 * - CalibrationRate: accuracy on Golden Set pairs
 * - AlignmentStability: consistency over rolling 50-eval window
 * - AuditPassRate: fraction of Audit Pairs answered consistently
 * - FraudFlags: detected anomalies (timing, position bias, collusion)
 *
 * Implements blended slashing:
 * - Golden Set (40%) + Audit (25%) + Consensus (20%) + Behavioral (15%)
 * - Above 60%: full rewards
 * - 40-60%: 50% reduced
 * - Below 40%: 10% slash + suspend
 *
 * PRD Reference: Section 4.6, 4.8
 */
@Service
public class CuratorScoringService {

    private static final Logger log = LoggerFactory.getLogger(CuratorScoringService.class);

    // Scoring weights
    private static final BigDecimal W1_CALIBRATION = new BigDecimal("0.40");
    private static final BigDecimal W2_ALIGNMENT = new BigDecimal("0.25");
    private static final BigDecimal W3_AUDIT = new BigDecimal("0.20");
    private static final BigDecimal W4_FRAUD = new BigDecimal("0.15");

    // Blended slashing weights
    private static final BigDecimal GOLDEN_WEIGHT = new BigDecimal("0.40");
    private static final BigDecimal AUDIT_WEIGHT = new BigDecimal("0.25");
    private static final BigDecimal CONSENSUS_WEIGHT = new BigDecimal("0.20");
    private static final BigDecimal BEHAVIORAL_WEIGHT = new BigDecimal("0.15");

    // Slashing thresholds
    private static final BigDecimal THRESHOLD_FULL_REWARDS = new BigDecimal("0.60");
    private static final BigDecimal THRESHOLD_REDUCED = new BigDecimal("0.40");
    private static final BigDecimal REDUCED_MULTIPLIER = new BigDecimal("0.50");
    private static final BigDecimal SLASH_MULTIPLIER = new BigDecimal("0.10");

    private final CuratorRepository curatorRepository;

    public CuratorScoringService(CuratorRepository curatorRepository) {
        this.curatorRepository = curatorRepository;
    }

    /**
     * Calculate curator score based on performance metrics.
     *
     * Formula: CuratorScore = w1*CalibrationRate + w2*AlignmentStability + w3*AuditPassRate - w4*FraudFlags
     *
     * @param curator The curator to score
     * @return Calculated curator score (0.0 to 1.0+)
     */
    public BigDecimal calculateCuratorScore(Curator curator) {
        BigDecimal calibrationRate = curator.getCalibrationRate() != null
                ? curator.getCalibrationRate()
                : BigDecimal.ZERO;

        BigDecimal alignmentStability = curator.getAlignmentStability() != null
                ? curator.getAlignmentStability()
                : BigDecimal.ZERO;

        BigDecimal auditPassRate = curator.getAuditPassRate() != null
                ? curator.getAuditPassRate()
                : BigDecimal.ZERO;

        // Normalize fraud flags: convert integer count to penalty (0.0 to 1.0)
        // Each fraud flag contributes 0.1 penalty, capped at 1.0
        BigDecimal fraudPenalty = BigDecimal.valueOf(Math.min(curator.getFraudFlags(), 10))
                .multiply(new BigDecimal("0.1"));

        BigDecimal score = calibrationRate.multiply(W1_CALIBRATION)
                .add(alignmentStability.multiply(W2_ALIGNMENT))
                .add(auditPassRate.multiply(W3_AUDIT))
                .subtract(fraudPenalty.multiply(W4_FRAUD));

        // Clamp score to [0.0, 1.0]
        score = score.max(BigDecimal.ZERO).min(BigDecimal.ONE);

        return score.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Calculate blended slashing score.
     *
     * Formula: BlendedScore = 0.40*GoldenSet + 0.25*Audit + 0.20*Consensus + 0.15*Behavioral
     *
     * @param goldenSetScore Golden set accuracy (0.0 to 1.0)
     * @param auditScore Audit pair accuracy (0.0 to 1.0)
     * @param consensusScore Consensus alignment (0.0 to 1.0)
     * @param behavioralScore Behavioral score (0.0 to 1.0)
     * @return Blended slashing score (0.0 to 1.0)
     */
    public BigDecimal calculateBlendedScore(
            BigDecimal goldenSetScore,
            BigDecimal auditScore,
            BigDecimal consensusScore,
            BigDecimal behavioralScore) {

        BigDecimal blended = goldenSetScore.multiply(GOLDEN_WEIGHT)
                .add(auditScore.multiply(AUDIT_WEIGHT))
                .add(consensusScore.multiply(CONSENSUS_WEIGHT))
                .add(behavioralScore.multiply(BEHAVIORAL_WEIGHT));

        return blended.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Apply slashing based on blended score.
     *
     * - Above 60%: full rewards (multiplier = 1.0)
     * - 40-60%: 50% reduced (multiplier = 0.5)
     * - Below 40%: 10% slash (multiplier = 0.1) + suspend
     *
     * @param blendedScore The blended slashing score (0.0 to 1.0)
     * @return Reward multiplier (0.1, 0.5, or 1.0)
     */
    public BigDecimal getRewardMultiplier(BigDecimal blendedScore) {
        if (blendedScore.compareTo(THRESHOLD_FULL_REWARDS) >= 0) {
            return BigDecimal.ONE;
        } else if (blendedScore.compareTo(THRESHOLD_REDUCED) >= 0) {
            return REDUCED_MULTIPLIER;
        } else {
            return SLASH_MULTIPLIER;
        }
    }

    /**
     * Check if curator should be suspended based on blended score.
     *
     * @param blendedScore The blended slashing score (0.0 to 1.0)
     * @return true if curator should be suspended (score below 40%)
     */
    public boolean shouldSuspend(BigDecimal blendedScore) {
        return blendedScore.compareTo(THRESHOLD_REDUCED) < 0;
    }

    /**
     * Update curator score and save to database.
     *
     * @param curator The curator to update
     * @return Updated curator with new score
     */
    @Transactional
    public Curator updateCuratorScore(Curator curator) {
        BigDecimal newScore = calculateCuratorScore(curator);
        curator.setCuratorScore(newScore);
        curator.setUpdatedAt(OffsetDateTime.now());

        Curator updated = curatorRepository.save(curator);

        log.info("Updated curator score: wallet={}, marketId={}, score={}",
                curator.getWallet(), curator.getMarketId(), newScore);

        return updated;
    }

    /**
     * Get voting power multiplier based on curator score.
     * High-score curators get voting power multiplier.
     *
     * @param curatorScore The curator's score (0.0 to 1.0)
     * @return Voting power multiplier (1.0 to 2.0)
     */
    public BigDecimal getVotingPowerMultiplier(BigDecimal curatorScore) {
        // Linear scaling: score 0.5 = 1.0x, score 1.0 = 2.0x
        BigDecimal multiplier = curatorScore.multiply(new BigDecimal("2.0"));

        // Clamp to [1.0, 2.0]
        multiplier = multiplier.max(BigDecimal.ONE).min(new BigDecimal("2.0"));

        return multiplier.setScale(4, RoundingMode.HALF_UP);
    }
}
