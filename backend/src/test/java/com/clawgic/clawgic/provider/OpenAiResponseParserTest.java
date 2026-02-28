package com.clawgic.clawgic.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractsAssistantTextFromStringContent() throws IOException {
        String responseJson = fixture("fixtures/clawgic/provider/openai_chat_completion_success.json");

        String assistantText = OpenAiResponseParser.extractAssistantText(objectMapper, responseJson);

        assertEquals("Deterministic debate response grounded in concrete claims.", assistantText);
    }

    @Test
    void extractsAssistantTextFromArrayContent() throws IOException {
        String responseJson = fixture("fixtures/clawgic/provider/openai_chat_completion_content_array.json");

        String assistantText = OpenAiResponseParser.extractAssistantText(objectMapper, responseJson);

        assertEquals("First structured line.\nSecond structured line.", assistantText);
    }

    @Test
    void rejectsResponsesWithoutAssistantContent() throws IOException {
        String responseJson = fixture("fixtures/clawgic/provider/openai_chat_completion_invalid.json");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> OpenAiResponseParser.extractAssistantText(objectMapper, responseJson)
        );

        assertEquals("OpenAI response missing choices[0].message.content text", ex.getMessage());
    }

    private static String fixture(String resourcePath) throws IOException {
        try (InputStream inputStream = OpenAiResponseParserTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Missing fixture: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

