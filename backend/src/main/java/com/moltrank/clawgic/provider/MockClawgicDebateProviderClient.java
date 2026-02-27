package com.moltrank.clawgic.provider;

import com.moltrank.clawgic.model.ClawgicProviderType;
import com.moltrank.clawgic.model.DebateTranscriptMessage;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic local mock provider for reproducible tests and demos.
 */
@Component
public class MockClawgicDebateProviderClient implements ClawgicDebateProviderClient {

    private static final int TOPIC_SNIPPET_WORD_LIMIT = 14;
    private static final int PROMPT_SNIPPET_WORD_LIMIT = 10;
    private static final int TRANSCRIPT_SNIPPET_WORD_LIMIT = 12;

    private static final List<String> OPENERS = List.of(
            "I anchor this round in verifiable claims.",
            "I will stay concrete and testable in this response.",
            "I focus on reproducible reasoning over rhetorical flourish.",
            "I will press the core assumptions with explicit evidence paths."
    );

    private static final List<String> EVIDENCE_STANCES = List.of(
            "frame the topic around measurable outcomes",
            "prioritize internal logical consistency",
            "stress adversarial stress-testing of weak premises",
            "tie each claim to falsifiable checkpoints"
    );

    private static final List<String> REBUTTAL_MOVES = List.of(
            "targeting ambiguity in the opposing framing",
            "challenging unsupported leaps in the prior turn",
            "isolating contradictions and resolving them directly",
            "reducing broad claims into testable sub-arguments"
    );

    private static final List<String> CLOSERS = List.of(
            "This keeps the debate deterministic and judge-readable.",
            "The result is concise, auditable, and phase-aligned.",
            "That structure should improve downstream judging reliability.",
            "This response is intentionally bounded for stable replay."
    );

    @Override
    public ClawgicProviderType providerType() {
        return ClawgicProviderType.MOCK;
    }

    @Override
    public ClawgicProviderTurnResponse generateTurn(ClawgicProviderTurnRequest request) {
        String seed = buildSeed(request);

        String opener = pick(OPENERS, seed, 0);
        String evidenceStance = pick(EVIDENCE_STANCES, seed, 1);
        String rebuttalMove = pick(REBUTTAL_MOVES, seed, 2);
        String closer = pick(CLOSERS, seed, 3);

        String phaseLabel = request.phase().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String topicSnippet = abbreviateWords(request.topic(), TOPIC_SNIPPET_WORD_LIMIT);
        String promptSnippet = abbreviateWords(request.systemPrompt(), PROMPT_SNIPPET_WORD_LIMIT);
        String transcriptSnippet = transcriptSnippet(request.transcript());

        String response = opener
                + " In the " + phaseLabel + " phase, I " + evidenceStance + " for: " + topicSnippet + ". "
                + "I follow persona guidance (" + promptSnippet + ") while " + rebuttalMove + ". "
                + "Transcript anchor: " + transcriptSnippet + ". "
                + closer;

        String boundedResponse = trimToWordLimit(response, request.maxWords());
        return new ClawgicProviderTurnResponse(boundedResponse, request.model());
    }

    private static String buildSeed(ClawgicProviderTurnRequest request) {
        return request.matchId()
                + "|"
                + request.agentId()
                + "|"
                + request.phase()
                + "|"
                + request.model()
                + "|"
                + request.topic()
                + "|"
                + request.systemPrompt()
                + "|"
                + request.transcript().size();
    }

    private static String pick(List<String> options, String seed, int salt) {
        int index = stableIndex(seed + "|" + salt, options.size());
        return options.get(index);
    }

    private static int stableIndex(String seed, int bound) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
            int rawValue = ByteBuffer.wrap(hash).getInt();
            return Math.floorMod(rawValue, bound);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest algorithm is required", ex);
        }
    }

    private static String transcriptSnippet(List<DebateTranscriptMessage> transcript) {
        if (transcript.isEmpty()) {
            return "no prior transcript turns";
        }
        DebateTranscriptMessage lastMessage = transcript.getLast();
        return abbreviateWords(lastMessage.content(), TRANSCRIPT_SNIPPET_WORD_LIMIT);
    }

    private static String abbreviateWords(String value, int maxWords) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        String[] words = normalized.split("\\s+");
        if (words.length <= maxWords) {
            return normalized;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(words[i]);
        }
        return builder.toString();
    }

    private static String trimToWordLimit(String value, int maxWords) {
        String normalized = value.trim();
        String[] words = normalized.split("\\s+");
        if (words.length <= maxWords) {
            return normalized;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(words[i]);
        }
        return builder.toString();
    }
}
