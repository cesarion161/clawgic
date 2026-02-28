package com.clawgic.clawgic.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnthropicResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractsAssistantTextFromAnthropicSegments() throws IOException {
        String responseJson = fixture("fixtures/clawgic/provider/anthropic_messages_success.json");

        String assistantText = AnthropicResponseParser.extractAssistantText(objectMapper, responseJson);

        assertEquals("First Anthropic segment.\nSecond Anthropic segment.", assistantText);
    }

    @Test
    void rejectsAnthropicResponsesWithoutTextSegments() throws IOException {
        String responseJson = fixture("fixtures/clawgic/provider/anthropic_messages_invalid.json");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> AnthropicResponseParser.extractAssistantText(objectMapper, responseJson)
        );

        assertEquals("Anthropic response content[] did not include text segments", ex.getMessage());
    }

    private static String fixture(String resourcePath) throws IOException {
        try (InputStream inputStream = AnthropicResponseParserTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Missing fixture: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

