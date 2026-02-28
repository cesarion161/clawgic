package com.clawgic.clawgic.provider;

import com.clawgic.clawgic.model.DebatePhase;
import com.clawgic.clawgic.model.DebateTranscriptMessage;
import com.clawgic.clawgic.model.DebateTranscriptRole;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockClawgicDebateProviderClientTest {

    private final MockClawgicDebateProviderClient mockProviderClient = new MockClawgicDebateProviderClient();

    @Test
    void generateTurnIsDeterministicForIdenticalInput() {
        ClawgicProviderTurnRequest request = request(
                UUID.fromString("00000000-0000-0000-0000-000000000701"),
                DebatePhase.ARGUMENTATION,
                80
        );

        ClawgicProviderTurnResponse first = mockProviderClient.generateTurn(request);
        ClawgicProviderTurnResponse second = mockProviderClient.generateTurn(request);

        assertEquals(first, second);
    }

    @Test
    void generateTurnVariesByAgentAndPhaseInputs() {
        ClawgicProviderTurnRequest first = request(
                UUID.fromString("00000000-0000-0000-0000-000000000702"),
                DebatePhase.THESIS_DISCOVERY,
                80
        );
        ClawgicProviderTurnRequest second = request(
                UUID.fromString("00000000-0000-0000-0000-000000000703"),
                DebatePhase.COUNTER_ARGUMENTATION,
                80
        );

        ClawgicProviderTurnResponse firstResponse = mockProviderClient.generateTurn(first);
        ClawgicProviderTurnResponse secondResponse = mockProviderClient.generateTurn(second);

        assertNotEquals(firstResponse.content(), secondResponse.content());
    }

    @Test
    void generateTurnRespectsWordLimit() {
        ClawgicProviderTurnRequest request = request(
                UUID.fromString("00000000-0000-0000-0000-000000000704"),
                DebatePhase.CONCLUSION,
                14
        );

        ClawgicProviderTurnResponse response = mockProviderClient.generateTurn(request);
        int wordCount = response.content().split("\\s+").length;

        assertTrue(wordCount <= 14, "expected <= 14 words but got " + wordCount);
    }

    private static ClawgicProviderTurnRequest request(UUID agentId, DebatePhase phase, int maxWords) {
        ClawgicProviderTurnInput input = new ClawgicProviderTurnInput(
                UUID.fromString("00000000-0000-0000-0000-000000000799"),
                agentId,
                phase,
                "Should hackathon demos prioritize deterministic mocks over live model variance?",
                "Argue in concise, falsifiable claims.",
                List.of(
                        new DebateTranscriptMessage(
                                DebateTranscriptRole.SYSTEM,
                                DebatePhase.THESIS_DISCOVERY,
                                "Phase context: respond directly and avoid filler."
                        ),
                        new DebateTranscriptMessage(
                                DebateTranscriptRole.AGENT_2,
                                DebatePhase.ARGUMENTATION,
                                "Live variance reveals whether a strategy survives production uncertainty."
                        )
                ),
                maxWords
        );

        return ClawgicProviderTurnRequest.fromInput(input, "clawgic-mock-v1", null, null);
    }
}
