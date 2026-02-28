package com.clawgic.clawgic.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Parses and repairs judge JSON payloads returned via OpenAI assistant content.
 */
final class OpenAiJudgeResultParser {

    private OpenAiJudgeResultParser() {
    }

    static ParseResult extractJudgeResult(ObjectMapper objectMapper, String responseBody) {
        String assistantText = OpenAiResponseParser.extractAssistantText(objectMapper, responseBody);
        ObjectNode judgeResult = parseJudgeResultObject(objectMapper, assistantText);
        return new ParseResult(judgeResult, assistantText);
    }

    private static ObjectNode parseJudgeResultObject(ObjectMapper objectMapper, String assistantText) {
        if (!StringUtils.hasText(assistantText)) {
            throw new IllegalArgumentException("OpenAI judge response content is empty");
        }

        Set<String> candidates = candidatePayloads(assistantText);
        IllegalArgumentException lastError = null;
        for (String candidate : candidates) {
            try {
                return parseObject(objectMapper, candidate);
            } catch (IllegalArgumentException ex) {
                lastError = ex;
            }
        }

        throw new IllegalArgumentException(
                "OpenAI judge response content is not a valid JSON object",
                lastError
        );
    }

    private static Set<String> candidatePayloads(String assistantText) {
        String normalized = assistantText.trim();
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        addCandidate(candidates, normalized);

        String unfenced = unwrapMarkdownFence(normalized);
        addCandidate(candidates, unfenced);

        String extractedFromNormalized = extractJsonObjectSlice(normalized);
        addCandidate(candidates, extractedFromNormalized);
        addCandidate(candidates, unwrapMarkdownFence(extractedFromNormalized));

        String extractedFromUnfenced = extractJsonObjectSlice(unfenced);
        addCandidate(candidates, extractedFromUnfenced);
        addCandidate(candidates, unwrapMarkdownFence(extractedFromUnfenced));

        return candidates;
    }

    private static void addCandidate(Set<String> candidates, String value) {
        if (StringUtils.hasText(value)) {
            candidates.add(value.trim());
        }
    }

    private static String unwrapMarkdownFence(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (!normalized.startsWith("```") || !normalized.endsWith("```")) {
            return normalized;
        }

        int headerBreak = normalized.indexOf('\n');
        if (headerBreak < 0) {
            return normalized;
        }
        String body = normalized.substring(headerBreak + 1, normalized.length() - 3).trim();
        return body;
    }

    private static String extractJsonObjectSlice(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        int firstBrace = value.indexOf('{');
        int lastBrace = value.lastIndexOf('}');
        if (firstBrace < 0 || lastBrace < 0 || lastBrace <= firstBrace) {
            return null;
        }
        return value.substring(firstBrace, lastBrace + 1);
    }

    private static ObjectNode parseObject(ObjectMapper objectMapper, String candidate) {
        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(candidate);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("OpenAI judge response content is not valid JSON", ex);
        }
        if (parsed == null || !parsed.isObject()) {
            throw new IllegalArgumentException("OpenAI judge response content must be a JSON object");
        }
        return ((ObjectNode) parsed).deepCopy();
    }

    record ParseResult(
            ObjectNode judgeResult,
            String assistantText
    ) {
    }
}
