package com.clawgic.clawgic.service;

import com.clawgic.clawgic.config.ClawgicProviderProperties;
import com.clawgic.clawgic.config.ClawgicRuntimeProperties;
import com.clawgic.clawgic.model.ClawgicAgent;
import com.clawgic.clawgic.model.ClawgicProviderType;
import com.clawgic.clawgic.model.DebatePhase;
import com.clawgic.clawgic.provider.ClawgicDebateProviderClient;
import com.clawgic.clawgic.provider.ClawgicProviderTurnInput;
import com.clawgic.clawgic.provider.ClawgicProviderTurnRequest;
import com.clawgic.clawgic.provider.ClawgicProviderTurnResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClawgicDebateProviderGatewayTest {

    @Mock
    private ClawgicAgentApiKeyCryptoService clawgicAgentApiKeyCryptoService;

    @Mock
    private ClawgicDebateProviderClient mockProviderClient;

    @Mock
    private ClawgicDebateProviderClient openaiProviderClient;

    @Mock
    private ClawgicDebateProviderClient anthropicProviderClient;

    private ClawgicRuntimeProperties clawgicRuntimeProperties;
    private ClawgicProviderProperties clawgicProviderProperties;
    private ClawgicDebateProviderGateway clawgicDebateProviderGateway;

    @BeforeEach
    void setUp() {
        clawgicRuntimeProperties = new ClawgicRuntimeProperties();
        clawgicRuntimeProperties.setMockProvider(false);

        clawgicProviderProperties = new ClawgicProviderProperties();

        when(mockProviderClient.providerType()).thenReturn(ClawgicProviderType.MOCK);
        when(openaiProviderClient.providerType()).thenReturn(ClawgicProviderType.OPENAI);
        when(anthropicProviderClient.providerType()).thenReturn(ClawgicProviderType.ANTHROPIC);

        clawgicDebateProviderGateway = new ClawgicDebateProviderGateway(
                List.of(mockProviderClient, openaiProviderClient, anthropicProviderClient),
                clawgicRuntimeProperties,
                clawgicProviderProperties,
                clawgicAgentApiKeyCryptoService
        );
    }

    @Test
    void resolveSelectionUsesAgentProviderAndKeyRefModelOverride() {
        clawgicProviderProperties.getKeyRefModels().put("team/anthropic/high", "claude-3-7-sonnet-latest");
        ClawgicAgent agent = agent(
                UUID.fromString("00000000-0000-0000-0000-000000000811"),
                ClawgicProviderType.ANTHROPIC,
                "team/anthropic/high"
        );

        ClawgicDebateProviderGateway.ProviderSelection selection = clawgicDebateProviderGateway.resolveSelection(agent);

        assertEquals(ClawgicProviderType.ANTHROPIC, selection.configuredProviderType());
        assertEquals(ClawgicProviderType.ANTHROPIC, selection.effectiveProviderType());
        assertEquals("team/anthropic/high", selection.providerKeyRef());
        assertEquals("claude-3-7-sonnet-latest", selection.model());
    }

    @Test
    void resolveSelectionForcesMockProviderWhenRuntimeMockModeEnabled() {
        clawgicRuntimeProperties.setMockProvider(true);
        ClawgicAgent agent = agent(
                UUID.fromString("00000000-0000-0000-0000-000000000812"),
                ClawgicProviderType.OPENAI,
                "team/openai/primary"
        );

        ClawgicDebateProviderGateway.ProviderSelection selection = clawgicDebateProviderGateway.resolveSelection(agent);

        assertEquals(ClawgicProviderType.OPENAI, selection.configuredProviderType());
        assertEquals(ClawgicProviderType.MOCK, selection.effectiveProviderType());
        assertEquals(clawgicProviderProperties.getMockModel(), selection.model());
    }

    @Test
    void generateTurnRoutesToConfiguredProviderAndDecryptsLiveApiKey() {
        ClawgicAgent agent = agent(
                UUID.fromString("00000000-0000-0000-0000-000000000813"),
                ClawgicProviderType.OPENAI,
                "team/openai/primary"
        );
        ClawgicProviderTurnInput input = turnInput(agent.getAgentId());

        when(clawgicAgentApiKeyCryptoService.decryptFromStorage(agent)).thenReturn("sk-live-route-openai");
        when(openaiProviderClient.generateTurn(any())).thenReturn(
                new ClawgicProviderTurnResponse("Deterministic OpenAI turn.", "gpt-4o-mini")
        );

        ClawgicProviderTurnResponse response = clawgicDebateProviderGateway.generateTurn(agent, input);

        assertEquals("Deterministic OpenAI turn.", response.content());

        ArgumentCaptor<ClawgicProviderTurnRequest> requestCaptor =
                ArgumentCaptor.forClass(ClawgicProviderTurnRequest.class);
        verify(openaiProviderClient).generateTurn(requestCaptor.capture());
        ClawgicProviderTurnRequest routedRequest = requestCaptor.getValue();
        assertEquals("gpt-4o-mini", routedRequest.model());
        assertEquals("sk-live-route-openai", routedRequest.providerApiKey());
        assertEquals("team/openai/primary", routedRequest.providerKeyRef());

        verify(clawgicAgentApiKeyCryptoService).decryptFromStorage(agent);
        verify(mockProviderClient, never()).generateTurn(any());
        verify(anthropicProviderClient, never()).generateTurn(any());
    }

    @Test
    void generateTurnUsesMockProviderWithoutDecryptingApiKey() {
        ClawgicAgent agent = agent(
                UUID.fromString("00000000-0000-0000-0000-000000000814"),
                ClawgicProviderType.MOCK,
                null
        );
        ClawgicProviderTurnInput input = turnInput(agent.getAgentId());

        when(mockProviderClient.generateTurn(any())).thenReturn(
                new ClawgicProviderTurnResponse("Deterministic mock turn.", clawgicProviderProperties.getMockModel())
        );

        ClawgicProviderTurnResponse response = clawgicDebateProviderGateway.generateTurn(agent, input);

        assertEquals("Deterministic mock turn.", response.content());

        ArgumentCaptor<ClawgicProviderTurnRequest> requestCaptor =
                ArgumentCaptor.forClass(ClawgicProviderTurnRequest.class);
        verify(mockProviderClient).generateTurn(requestCaptor.capture());
        ClawgicProviderTurnRequest routedRequest = requestCaptor.getValue();
        assertEquals(clawgicProviderProperties.getMockModel(), routedRequest.model());
        assertNull(routedRequest.providerApiKey());

        verifyNoInteractions(clawgicAgentApiKeyCryptoService);
        verify(openaiProviderClient, never()).generateTurn(any());
        verify(anthropicProviderClient, never()).generateTurn(any());
    }

    @Test
    void resolveSelectionFailsWhenProviderClientIsMissing() {
        ClawgicDebateProviderGateway gatewayWithoutOpenai = new ClawgicDebateProviderGateway(
                List.of(mockProviderClient),
                clawgicRuntimeProperties,
                clawgicProviderProperties,
                clawgicAgentApiKeyCryptoService
        );

        ClawgicAgent openaiAgent = agent(
                UUID.fromString("00000000-0000-0000-0000-000000000815"),
                ClawgicProviderType.OPENAI,
                "team/openai/primary"
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> gatewayWithoutOpenai.resolveSelection(openaiAgent)
        );

        assertEquals("No Clawgic provider client registered for provider type: OPENAI", ex.getMessage());
    }

    private static ClawgicProviderTurnInput turnInput(UUID agentId) {
        return new ClawgicProviderTurnInput(
                UUID.fromString("00000000-0000-0000-0000-000000000899"),
                agentId,
                DebatePhase.ARGUMENTATION,
                "Should deterministic mocks be mandatory for a hackathon tournament pipeline?",
                "Argue concisely and cite falsifiable checkpoints.",
                List.of(),
                120
        );
    }

    private static ClawgicAgent agent(UUID agentId, ClawgicProviderType providerType, String providerKeyRef) {
        ClawgicAgent agent = new ClawgicAgent();
        agent.setAgentId(agentId);
        agent.setProviderType(providerType);
        agent.setProviderKeyRef(providerKeyRef);
        agent.setApiKeyEncrypted("{\"kid\":\"local-dev-v1\"}");
        return agent;
    }
}
