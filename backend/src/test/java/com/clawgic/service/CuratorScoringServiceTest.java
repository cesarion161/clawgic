package com.clawgic.service;

import com.clawgic.model.Curator;
import com.clawgic.repository.CuratorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuratorScoringServiceTest {

    @Mock
    private CuratorRepository curatorRepository;

    @InjectMocks
    private CuratorScoringService scoringService;

    private Curator curator;

    @BeforeEach
    void setUp() {
        curator = new Curator();
        curator.setWallet("4Nd1mYQzvgV8Vr3Z3nYb7pD6T8K9jF2eqWxY1S3Qh5Ro");
        curator.setMarketId(1);
        curator.setIdentityId(1);
        curator.setFraudFlags(0);
    }

    @Test
    void calculateCuratorScore_perfectCurator() {
        curator.setCalibrationRate(new BigDecimal("1.0"));
        curator.setAlignmentStability(new BigDecimal("1.0"));
        curator.setAuditPassRate(new BigDecimal("1.0"));
        curator.setFraudFlags(0);

        BigDecimal score = scoringService.calculateCuratorScore(curator);

        // 0.40*1.0 + 0.25*1.0 + 0.20*1.0 - 0.15*0 = 0.85
        assertEquals(new BigDecimal("0.8500"), score);
    }

    @Test
    void calculateCuratorScore_zeroCurator() {
        curator.setCalibrationRate(BigDecimal.ZERO);
        curator.setAlignmentStability(BigDecimal.ZERO);
        curator.setAuditPassRate(BigDecimal.ZERO);
        curator.setFraudFlags(0);

        BigDecimal score = scoringService.calculateCuratorScore(curator);

        assertEquals(new BigDecimal("0.0000"), score);
    }

    @Test
    void calculateCuratorScore_withFraudFlags() {
        curator.setCalibrationRate(new BigDecimal("0.90"));
        curator.setAlignmentStability(new BigDecimal("0.80"));
        curator.setAuditPassRate(new BigDecimal("0.70"));
        curator.setFraudFlags(3);

        BigDecimal score = scoringService.calculateCuratorScore(curator);

        // 0.40*0.90 + 0.25*0.80 + 0.20*0.70 - 0.15*(3*0.1)
        // = 0.36 + 0.20 + 0.14 - 0.045 = 0.655
        assertEquals(new BigDecimal("0.6550"), score);
    }

    @Test
    void calculateCuratorScore_fraudFlagsCappedAt10() {
        curator.setCalibrationRate(new BigDecimal("1.0"));
        curator.setAlignmentStability(new BigDecimal("1.0"));
        curator.setAuditPassRate(new BigDecimal("1.0"));
        curator.setFraudFlags(20); // More than cap of 10

        BigDecimal score = scoringService.calculateCuratorScore(curator);

        // 0.40 + 0.25 + 0.20 - 0.15*1.0 = 0.70
        assertEquals(new BigDecimal("0.7000"), score);
    }

    @Test
    void calculateCuratorScore_clampedToZero() {
        curator.setCalibrationRate(BigDecimal.ZERO);
        curator.setAlignmentStability(BigDecimal.ZERO);
        curator.setAuditPassRate(BigDecimal.ZERO);
        curator.setFraudFlags(10);

        BigDecimal score = scoringService.calculateCuratorScore(curator);

        // 0 - 0.15*1.0 = -0.15, clamped to 0
        assertEquals(new BigDecimal("0.0000"), score);
    }

    @Test
    void calculateCuratorScore_nullFields_treatedAsZero() {
        curator.setCalibrationRate(null);
        curator.setAlignmentStability(null);
        curator.setAuditPassRate(null);
        curator.setFraudFlags(0);

        BigDecimal score = scoringService.calculateCuratorScore(curator);

        assertEquals(new BigDecimal("0.0000"), score);
    }

    @Test
    void calculateBlendedScore_allPerfect() {
        BigDecimal blended = scoringService.calculateBlendedScore(
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);

        // 0.40 + 0.25 + 0.20 + 0.15 = 1.00
        assertEquals(new BigDecimal("1.0000"), blended);
    }

    @Test
    void calculateBlendedScore_mixedScores() {
        BigDecimal blended = scoringService.calculateBlendedScore(
                new BigDecimal("0.80"), new BigDecimal("0.60"),
                new BigDecimal("0.70"), new BigDecimal("0.90"));

        // 0.40*0.80 + 0.25*0.60 + 0.20*0.70 + 0.15*0.90
        // = 0.32 + 0.15 + 0.14 + 0.135 = 0.745
        assertEquals(new BigDecimal("0.7450"), blended);
    }

    @Test
    void getRewardMultiplier_above60percent_fullRewards() {
        assertEquals(BigDecimal.ONE, scoringService.getRewardMultiplier(new BigDecimal("0.85")));
        assertEquals(BigDecimal.ONE, scoringService.getRewardMultiplier(new BigDecimal("0.60")));
        assertEquals(BigDecimal.ONE, scoringService.getRewardMultiplier(new BigDecimal("1.00")));
    }

    @Test
    void getRewardMultiplier_between40and60_reducedRewards() {
        assertEquals(new BigDecimal("0.50"), scoringService.getRewardMultiplier(new BigDecimal("0.50")));
        assertEquals(new BigDecimal("0.50"), scoringService.getRewardMultiplier(new BigDecimal("0.40")));
        assertEquals(new BigDecimal("0.50"), scoringService.getRewardMultiplier(new BigDecimal("0.59")));
    }

    @Test
    void getRewardMultiplier_below40_slashed() {
        assertEquals(new BigDecimal("0.10"), scoringService.getRewardMultiplier(new BigDecimal("0.39")));
        assertEquals(new BigDecimal("0.10"), scoringService.getRewardMultiplier(new BigDecimal("0.10")));
        assertEquals(new BigDecimal("0.10"), scoringService.getRewardMultiplier(BigDecimal.ZERO));
    }

    @Test
    void shouldSuspend_below40_true() {
        assertTrue(scoringService.shouldSuspend(new BigDecimal("0.39")));
        assertTrue(scoringService.shouldSuspend(new BigDecimal("0.10")));
        assertTrue(scoringService.shouldSuspend(BigDecimal.ZERO));
    }

    @Test
    void shouldSuspend_atOrAbove40_false() {
        assertFalse(scoringService.shouldSuspend(new BigDecimal("0.40")));
        assertFalse(scoringService.shouldSuspend(new BigDecimal("0.60")));
        assertFalse(scoringService.shouldSuspend(BigDecimal.ONE));
    }

    @Test
    void getVotingPowerMultiplier_score1_returns2x() {
        BigDecimal multiplier = scoringService.getVotingPowerMultiplier(BigDecimal.ONE);
        assertEquals(new BigDecimal("2.0000"), multiplier);
    }

    @Test
    void getVotingPowerMultiplier_score05_returns1x() {
        BigDecimal multiplier = scoringService.getVotingPowerMultiplier(new BigDecimal("0.50"));
        assertEquals(new BigDecimal("1.0000"), multiplier);
    }

    @Test
    void getVotingPowerMultiplier_lowScore_clampedAt1x() {
        BigDecimal multiplier = scoringService.getVotingPowerMultiplier(new BigDecimal("0.20"));
        assertEquals(new BigDecimal("1.0000"), multiplier);
    }

    @Test
    void updateCuratorScore_savesToRepository() {
        curator.setCalibrationRate(new BigDecimal("0.90"));
        curator.setAlignmentStability(new BigDecimal("0.85"));
        curator.setAuditPassRate(new BigDecimal("0.75"));
        curator.setFraudFlags(0);

        when(curatorRepository.save(any(Curator.class))).thenAnswer(inv -> inv.getArgument(0));

        Curator updated = scoringService.updateCuratorScore(curator);

        assertNotNull(updated.getCuratorScore());
        assertTrue(updated.getCuratorScore().compareTo(BigDecimal.ZERO) > 0);
        verify(curatorRepository).save(curator);
    }
}
