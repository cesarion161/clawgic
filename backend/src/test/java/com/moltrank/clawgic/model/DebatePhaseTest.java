package com.moltrank.clawgic.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DebatePhaseTest {

    @Test
    void orderedValuesExposesStablePhaseOrder() {
        assertEquals(
                List.of(
                        DebatePhase.THESIS_DISCOVERY,
                        DebatePhase.ARGUMENTATION,
                        DebatePhase.COUNTER_ARGUMENTATION,
                        DebatePhase.CONCLUSION
                ),
                DebatePhase.orderedValues()
        );
    }
}
