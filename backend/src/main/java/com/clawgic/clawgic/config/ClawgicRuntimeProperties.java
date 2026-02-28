package com.clawgic.clawgic.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Clawgic runtime feature flags and execution defaults.
 * These are introduced ahead of the Clawgic domain implementation to allow
 * incremental pivot work without breaking legacy flows.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clawgic")
public class ClawgicRuntimeProperties {

    /**
     * Master feature flag for Clawgic endpoints/workers.
     */
    private boolean enabled = true;

    /**
     * Run provider calls in deterministic mock mode (fixture/seeded output).
     */
    private boolean mockProvider = true;

    /**
     * Run judge in deterministic mock mode for reproducible tests/demo.
     */
    private boolean mockJudge = true;

    /**
     * Allows hiding legacy UI flows from the demo shell without deleting code.
     */
    private boolean legacyUiEnabled = true;

    /**
     * Allows gating legacy APIs off the demo path in future steps.
     */
    private boolean legacyApiEnabled = true;

    private Tournament tournament = new Tournament();
    private Worker worker = new Worker();
    private Debate debate = new Debate();

    @Getter
    @Setter
    public static class Tournament {
        private int mvpBracketSize = 4;
        private int defaultEntryWindowMinutes = 60;
        private BigDecimal judgeFeeUsdcPerCompletedMatch = new BigDecimal("0.250000");
        private BigDecimal systemRetentionRate = new BigDecimal("0.000000");
    }

    @Getter
    @Setter
    public static class Worker {
        private boolean enabled = true;
        private long initialDelayMs = 5_000;
        private long pollIntervalMs = 10_000;
        private String queueMode = "redis";
        private String redisQueueKey = "clawgic:judge:queue";
        private long redisPopTimeoutSeconds = 1;
    }

    @Getter
    @Setter
    public static class Debate {
        private int maxExchangesPerAgent = 3;
        private int maxResponseWords = 180;
        private int maxResponseTokens = 512;
        private int providerTimeoutSeconds = 15;
    }
}
