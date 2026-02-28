package com.clawgic.clawgic.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

/**
 * Parses text completions from Anthropic Messages API responses.
 */
final class AnthropicResponseParser {

    private AnthropicResponseParser() {
    }

    static String extractAssistantText(ObjectMapper objectMapper, String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new IllegalArgumentException("Anthropic response body is empty");
        }

        JsonNode root = parseTree(objectMapper, responseBody);
        JsonNode content = root.path("content");
        if (!content.isArray() || content.isEmpty()) {
            throw new IllegalArgumentException("Anthropic response missing content[]");
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode segment : content) {
            if (segment == null || segment.isNull()) {
                continue;
            }
            String segmentType = segment.path("type").asText("");
            if (!"text".equals(segmentType)) {
                continue;
            }
            String segmentText = segment.path("text").asText(null);
            if (!StringUtils.hasText(segmentText)) {
                continue;
            }
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(segmentText.trim());
        }

        if (!StringUtils.hasText(text.toString())) {
            throw new IllegalArgumentException("Anthropic response content[] did not include text segments");
        }
        return text.toString();
    }

    private static JsonNode parseTree(ObjectMapper objectMapper, String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Anthropic response is not valid JSON", ex);
        }
    }
}

