package com.clawgic.clawgic.service;

import com.clawgic.clawgic.config.ClawgicProviderProperties;
import com.clawgic.clawgic.config.ClawgicRuntimeProperties;
import com.clawgic.clawgic.model.ClawgicAgent;
import com.clawgic.clawgic.model.ClawgicProviderType;
import com.clawgic.clawgic.provider.ClawgicDebateProviderClient;
import com.clawgic.clawgic.provider.ClawgicProviderTurnInput;
import com.clawgic.clawgic.provider.ClawgicProviderTurnRequest;
import com.clawgic.clawgic.provider.ClawgicProviderTurnResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves provider routing per-agent and delegates turn generation to provider clients.
 */
@Service
public class ClawgicDebateProviderGateway {

    private final Map<ClawgicProviderType, ClawgicDebateProviderClient> providerClientsByType;
    private final ClawgicRuntimeProperties clawgicRuntimeProperties;
    private final ClawgicProviderProperties clawgicProviderProperties;
    private final ClawgicAgentApiKeyCryptoService clawgicAgentApiKeyCryptoService;

    public ClawgicDebateProviderGateway(
            List<ClawgicDebateProviderClient> providerClients,
            ClawgicRuntimeProperties clawgicRuntimeProperties,
            ClawgicProviderProperties clawgicProviderProperties,
            ClawgicAgentApiKeyCryptoService clawgicAgentApiKeyCryptoService
    ) {
        this.providerClientsByType = indexProviderClients(providerClients);
        this.clawgicRuntimeProperties = clawgicRuntimeProperties;
        this.clawgicProviderProperties = clawgicProviderProperties;
        this.clawgicAgentApiKeyCryptoService = clawgicAgentApiKeyCryptoService;
    }

    public ProviderSelection resolveSelection(ClawgicAgent agent) {
        Objects.requireNonNull(agent, "agent is required");

        ClawgicProviderType configuredProviderType = agent.getProviderType() == null
                ? ClawgicProviderType.OPENAI
                : agent.getProviderType();
        ClawgicProviderType effectiveProviderType = clawgicRuntimeProperties.isMockProvider()
                ? ClawgicProviderType.MOCK
                : configuredProviderType;
        String providerKeyRef = normalizeOptional(agent.getProviderKeyRef());
        String model = resolveModel(effectiveProviderType, providerKeyRef);

        requireProviderClient(effectiveProviderType);
        return new ProviderSelection(configuredProviderType, effectiveProviderType, model, providerKeyRef);
    }

    public ClawgicProviderTurnResponse generateTurn(ClawgicAgent agent, ClawgicProviderTurnInput turnInput) {
        Objects.requireNonNull(agent, "agent is required");
        Objects.requireNonNull(turnInput, "turnInput is required");

        if (agent.getAgentId() == null) {
            throw new IllegalArgumentException("agentId is required");
        }
        if (!agent.getAgentId().equals(turnInput.agentId())) {
            throw new IllegalArgumentException("turnInput.agentId must match the selected agent");
        }

        ProviderSelection selection = resolveSelection(agent);
        ClawgicDebateProviderClient providerClient = requireProviderClient(selection.effectiveProviderType());

        String providerApiKey = selection.effectiveProviderType() == ClawgicProviderType.MOCK
                ? null
                : clawgicAgentApiKeyCryptoService.decryptFromStorage(agent);
        ClawgicProviderTurnRequest request = ClawgicProviderTurnRequest.fromInput(
                turnInput,
                selection.model(),
                providerApiKey,
                selection.providerKeyRef()
        );

        return providerClient.generateTurn(request);
    }

    private String resolveModel(ClawgicProviderType providerType, String providerKeyRef) {
        if (providerType != ClawgicProviderType.MOCK && StringUtils.hasText(providerKeyRef)) {
            String keyRefModel = clawgicProviderProperties.getKeyRefModels().get(providerKeyRef);
            if (StringUtils.hasText(keyRefModel)) {
                return keyRefModel.trim();
            }
        }

        return switch (providerType) {
            case OPENAI -> clawgicProviderProperties.getOpenaiDefaultModel();
            case ANTHROPIC -> clawgicProviderProperties.getAnthropicDefaultModel();
            case MOCK -> clawgicProviderProperties.getMockModel();
        };
    }

    private ClawgicDebateProviderClient requireProviderClient(ClawgicProviderType providerType) {
        ClawgicDebateProviderClient providerClient = providerClientsByType.get(providerType);
        if (providerClient == null) {
            throw new IllegalStateException("No Clawgic provider client registered for provider type: " + providerType);
        }
        return providerClient;
    }

    private static Map<ClawgicProviderType, ClawgicDebateProviderClient> indexProviderClients(
            List<ClawgicDebateProviderClient> providerClients
    ) {
        Objects.requireNonNull(providerClients, "providerClients is required");
        EnumMap<ClawgicProviderType, ClawgicDebateProviderClient> index = new EnumMap<>(ClawgicProviderType.class);
        for (ClawgicDebateProviderClient providerClient : providerClients) {
            ClawgicProviderType providerType = providerClient.providerType();
            if (index.containsKey(providerType)) {
                throw new IllegalStateException("Duplicate Clawgic provider client registration for: " + providerType);
            }
            index.put(providerType, providerClient);
        }
        return Map.copyOf(index);
    }

    private static String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public record ProviderSelection(
            ClawgicProviderType configuredProviderType,
            ClawgicProviderType effectiveProviderType,
            String model,
            String providerKeyRef
    ) {
    }
}
