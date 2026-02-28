package com.clawgic.clawgic.provider;

import com.clawgic.clawgic.model.DebateTranscriptMessage;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Builds provider-neutral prompt context for one debate turn.
 */
final class ClawgicProviderPromptComposer {

    private static final int TRANSCRIPT_TAIL_LIMIT = 24;

    private ClawgicProviderPromptComposer() {
    }

    static String toUserPrompt(ClawgicProviderTurnRequest request) {
        StringBuilder prompt = new StringBuilder()
                .append("Topic:\n")
                .append(request.topic())
                .append("\n\n")
                .append("Current phase:\n")
                .append(request.phase().name())
                .append("\n\n")
                .append("Debate transcript so far (chronological):\n")
                .append(renderTranscriptTail(request.transcript()))
                .append("\n\n")
                .append("Instruction:\n")
                .append("Write your next turn in 1-2 short paragraphs, with no bullet points and no markdown. ")
                .append("Keep it concrete and adversarial. Hard word cap: ")
                .append(request.maxWords())
                .append(" words.");

        if (StringUtils.hasText(request.providerKeyRef())) {
            prompt.append("\nProvider profile key: ").append(request.providerKeyRef());
        }
        return prompt.toString();
    }

    private static String renderTranscriptTail(List<DebateTranscriptMessage> transcript) {
        if (transcript.isEmpty()) {
            return "(no prior turns)";
        }
        int start = Math.max(0, transcript.size() - TRANSCRIPT_TAIL_LIMIT);
        StringBuilder rendered = new StringBuilder();
        for (int index = start; index < transcript.size(); index++) {
            DebateTranscriptMessage message = transcript.get(index);
            if (!rendered.isEmpty()) {
                rendered.append('\n');
            }
            rendered.append(index + 1)
                    .append(". [")
                    .append(message.role().name())
                    .append(" | ")
                    .append(message.phase().name())
                    .append("] ")
                    .append(message.content());
        }
        return rendered.toString();
    }
}

