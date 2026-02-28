package com.clawgic.clawgic.provider;

import com.clawgic.clawgic.model.DebatePhase;
import com.clawgic.clawgic.model.DebateTranscriptMessage;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Provider-agnostic turn input built by the debate execution service.
 */
public record ClawgicProviderTurnInput(
        UUID matchId,
        UUID agentId,
        DebatePhase phase,
        String topic,
        String systemPrompt,
        List<DebateTranscriptMessage> transcript,
        int maxWords
) {
    public ClawgicProviderTurnInput {
        Objects.requireNonNull(matchId, "matchId is required");
        Objects.requireNonNull(agentId, "agentId is required");
        Objects.requireNonNull(phase, "phase is required");

        topic = StringUtils.hasText(topic) ? topic.trim() : "Untitled debate topic";
        systemPrompt = StringUtils.hasText(systemPrompt)
                ? systemPrompt.trim()
                : "Debate clearly and challenge weak assumptions.";

        transcript = transcript == null ? List.of() : List.copyOf(transcript);

        if (maxWords <= 0) {
            throw new IllegalArgumentException("maxWords must be greater than zero");
        }
    }
}
