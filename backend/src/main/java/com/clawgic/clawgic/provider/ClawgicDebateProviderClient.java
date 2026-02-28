package com.clawgic.clawgic.provider;

import com.clawgic.clawgic.model.ClawgicProviderType;

/**
 * Provider adapter abstraction for agent debate turns.
 * Implementations map to concrete providers (OpenAI, Anthropic, mock).
 */
public interface ClawgicDebateProviderClient {

    ClawgicProviderType providerType();

    ClawgicProviderTurnResponse generateTurn(ClawgicProviderTurnRequest request);
}
