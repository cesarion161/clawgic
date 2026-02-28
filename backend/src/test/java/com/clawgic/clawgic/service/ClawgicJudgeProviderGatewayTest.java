package com.clawgic.clawgic.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.clawgic.clawgic.config.ClawgicJudgeProperties;
import com.clawgic.clawgic.config.ClawgicRuntimeProperties;
import com.clawgic.clawgic.model.ClawgicMatch;
import com.clawgic.clawgic.model.ClawgicTournament;
import com.clawgic.clawgic.model.DebatePhase;
import com.clawgic.clawgic.model.DebateTranscriptMessage;
import com.clawgic.clawgic.model.DebateTranscriptRole;
import com.clawgic.clawgic.provider.ClawgicJudgeRequest;
import com.clawgic.clawgic.provider.MockClawgicJudgeProviderClient;
import com.clawgic.clawgic.provider.OpenAiClawgicJudgeProviderClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClawgicJudgeProviderGatewayTest {

    @Mock
    private MockClawgicJudgeProviderClient mockJudgeProviderClient;

    @Mock
    private OpenAiClawgicJudgeProviderClient openAiJudgeProviderClient;

    private ClawgicRuntimeProperties clawgicRuntimeProperties;
    private ClawgicJudgeProperties clawgicJudgeProperties;
    private ClawgicJudgeProviderGateway clawgicJudgeProviderGateway;

    @BeforeEach
    void setUp() {
        clawgicRuntimeProperties = new ClawgicRuntimeProperties();
        clawgicJudgeProperties = new ClawgicJudgeProperties();
        clawgicJudgeProviderGateway = new ClawgicJudgeProviderGateway(
                clawgicRuntimeProperties,
                clawgicJudgeProperties,
                mockJudgeProviderClient,
                openAiJudgeProviderClient
        );
    }

    @Test
    void evaluateUsesMockProviderWhenMockJudgeModeEnabled() {
        clawgicRuntimeProperties.setMockJudge(true);
        ObjectNode mockResult = JsonNodeFactory.instance.objectNode().put("winner_id", UUID.randomUUID().toString());
        when(mockJudgeProviderClient.evaluate(any())).thenReturn(mockResult);

        ClawgicJudgeProviderGateway.JudgeEvaluation evaluation = clawgicJudgeProviderGateway.evaluate(
                match(),
                tournament(),
                "mock-judge-primary",
                transcript()
        );

        assertEquals(mockResult, evaluation.resultJson());
        assertEquals(clawgicJudgeProperties.getModel(), evaluation.model());

        ArgumentCaptor<ClawgicJudgeRequest> requestCaptor = ArgumentCaptor.forClass(ClawgicJudgeRequest.class);
        verify(mockJudgeProviderClient).evaluate(requestCaptor.capture());
        assertEquals("mock-judge-primary", requestCaptor.getValue().judgeKey());
        verify(openAiJudgeProviderClient, never()).evaluate(any());
    }

    @Test
    void evaluateUsesOpenAiProviderWhenMockJudgeModeDisabled() {
        clawgicRuntimeProperties.setMockJudge(false);
        clawgicJudgeProperties.setProvider("openai");
        clawgicJudgeProperties.setModel("gpt-4.1");

        ObjectNode liveResult = JsonNodeFactory.instance.objectNode().put("winner_id", UUID.randomUUID().toString());
        when(openAiJudgeProviderClient.evaluate(any())).thenReturn(liveResult);

        ClawgicJudgeProviderGateway.JudgeEvaluation evaluation = clawgicJudgeProviderGateway.evaluate(
                match(),
                tournament(),
                "live-judge-primary",
                transcript()
        );

        assertEquals(liveResult, evaluation.resultJson());
        assertEquals("gpt-4.1", evaluation.model());

        ArgumentCaptor<ClawgicJudgeRequest> requestCaptor = ArgumentCaptor.forClass(ClawgicJudgeRequest.class);
        verify(openAiJudgeProviderClient).evaluate(requestCaptor.capture());
        assertEquals("live-judge-primary", requestCaptor.getValue().judgeKey());
        verify(mockJudgeProviderClient, never()).evaluate(any());
    }

    @Test
    void evaluateFailsForUnsupportedLiveJudgeProvider() {
        clawgicRuntimeProperties.setMockJudge(false);
        clawgicJudgeProperties.setProvider("anthropic");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> clawgicJudgeProviderGateway.evaluate(match(), tournament(), "judge-key", transcript())
        );

        assertEquals("Unsupported clawgic.judge.provider: anthropic", ex.getMessage());
    }

    private static ClawgicMatch match() {
        ClawgicMatch match = new ClawgicMatch();
        match.setMatchId(UUID.fromString("00000000-0000-0000-0000-000000000931"));
        match.setTournamentId(UUID.fromString("00000000-0000-0000-0000-000000000932"));
        match.setAgent1Id(UUID.fromString("00000000-0000-0000-0000-000000000933"));
        match.setAgent2Id(UUID.fromString("00000000-0000-0000-0000-000000000934"));
        return match;
    }

    private static ClawgicTournament tournament() {
        ClawgicTournament tournament = new ClawgicTournament();
        tournament.setTournamentId(UUID.fromString("00000000-0000-0000-0000-000000000932"));
        tournament.setTopic("Should strict JSON mode be mandatory for automated judging?");
        return tournament;
    }

    private static List<DebateTranscriptMessage> transcript() {
        return List.of(
                new DebateTranscriptMessage(
                        DebateTranscriptRole.AGENT_1,
                        DebatePhase.ARGUMENTATION,
                        "Strict outputs reduce parser ambiguity and production risk."
                ),
                new DebateTranscriptMessage(
                        DebateTranscriptRole.AGENT_2,
                        DebatePhase.COUNTER_ARGUMENTATION,
                        "Loose outputs can preserve nuance but require robust repair logic."
                )
        );
    }
}
