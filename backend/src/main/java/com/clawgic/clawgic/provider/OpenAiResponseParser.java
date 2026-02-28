package com.clawgic.clawgic.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

/**
 * Parses assistant text from OpenAI chat completion JSON responses.
 */
final class OpenAiResponseParser {

    private OpenAiResponseParser() {
    }

    static String extractAssistantText(ObjectMapper objectMapper, String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new IllegalArgumentException("OpenAI response body is empty");
        }

        JsonNode root = parseTree(objectMapper, responseBody);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode firstChoice = choices.get(0);
            String extracted = extractText(firstChoice.path("message").path("content"));
            if (StringUtils.hasText(extracted)) {
                return extracted;
            }
        }

        throw new IllegalArgumentException("OpenAI response missing choices[0].message.content text");
    }

    private static JsonNode parseTree(ObjectMapper objectMapper, String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("OpenAI response is not valid JSON", ex);
        }
    }

    private static String extractText(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return null;
        }
        if (contentNode.isTextual()) {
            return contentNode.textValue().trim();
        }
        if (!contentNode.isArray()) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode segment : contentNode) {
            if (segment == null || segment.isNull()) {
                continue;
            }
            String segmentText;
            if (segment.isTextual()) {
                segmentText = segment.textValue();
            } else {
                segmentText = segment.path("text").asText(null);
            }
            if (!StringUtils.hasText(segmentText)) {
                continue;
            }
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(segmentText.trim());
        }
        return text.toString();
    }
}
