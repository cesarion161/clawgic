package com.clawgic.clawgic.provider;

import com.clawgic.clawgic.model.DebatePhase;
import com.clawgic.clawgic.model.DebateTranscriptMessage;
import org.jspecify.annotations.NonNull;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Fully-resolved provider request, including routing metadata and credentials.
 */
public record ClawgicProviderTurnRequest(
        UUID matchId,
        UUID agentId,
        DebatePhase phase,
        String topic,
        String systemPrompt,
        List<DebateTranscriptMessage> transcript,
        int maxWords,
        String model,
        String providerApiKey,
        String providerKeyRef
) {
    public ClawgicProviderTurnRequest {
        Objects.requireNonNull(matchId, "matchId is required");
        Objects.requireNonNull(agentId, "agentId is required");
        Objects.requireNonNull(phase, "phase is required");

        if (!StringUtils.hasText(topic)) {
            throw new IllegalArgumentException("topic is required");
        }
        topic = topic.trim();

        if (!StringUtils.hasText(systemPrompt)) {
            throw new IllegalArgumentException("systemPrompt is required");
        }
        systemPrompt = systemPrompt.trim();

        transcript = transcript == null ? List.of() : List.copyOf(transcript);

        if (maxWords <= 0) {
            throw new IllegalArgumentException("maxWords must be greater than zero");
        }

        if (!StringUtils.hasText(model)) {
            throw new IllegalArgumentException("model is required");
        }
        model = model.trim();

        providerApiKey = StringUtils.hasText(providerApiKey) ? providerApiKey.trim() : null;
        providerKeyRef = StringUtils.hasText(providerKeyRef) ? providerKeyRef.trim() : null;
    }

    public static ClawgicProviderTurnRequest fromInput(
            ClawgicProviderTurnInput input,
            String model,
            String providerApiKey,
            String providerKeyRef
    ) {
        Objects.requireNonNull(input, "input is required");
        return new ClawgicProviderTurnRequest(
                input.matchId(),
                input.agentId(),
                input.phase(),
                input.topic(),
                input.systemPrompt(),
                input.transcript(),
                input.maxWords(),
                model,
                providerApiKey,
                providerKeyRef
        );
    }

    @Override
    public @NonNull String toString() {
        return "ClawgicProviderTurnRequest[matchId="
                + matchId
                + ", agentId="
                + agentId
                + ", phase="
                + phase
                + ", model="
                + model
                + ", providerApiKey=<redacted>, providerKeyRef="
                + providerKeyRef
                + ']';
    }
}
