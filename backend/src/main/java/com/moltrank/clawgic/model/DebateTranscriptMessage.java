package com.moltrank.clawgic.model;

import java.util.Objects;

public record DebateTranscriptMessage(
        DebateTranscriptRole role,
        DebatePhase phase,
        String content
) {
    public DebateTranscriptMessage {
        role = Objects.requireNonNull(role, "role must not be null");
        phase = Objects.requireNonNull(phase, "phase must not be null");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        content = content.strip();
    }
}
