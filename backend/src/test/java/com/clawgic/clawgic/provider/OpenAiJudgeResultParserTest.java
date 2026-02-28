package com.clawgic.clawgic.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiJudgeResultParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesPlainJsonAssistantContent() throws IOException {
        String responseJson = fixture("fixtures/clawgic/provider/openai_judge_success.json");

        OpenAiJudgeResultParser.ParseResult parseResult =
                OpenAiJudgeResultParser.extractJudgeResult(objectMapper, responseJson);

        ObjectNode result = parseResult.judgeResult();
        assertEquals("00000000-0000-0000-0000-000000000901", result.path("winner_id").asText());
        assertEquals(8, result.path("agent_1").path("logic").asInt());
        assertEquals(6, result.path("agent_2").path("rebuttal_strength").asInt());
    }

    @Test
    void repairsMarkdownFencedJsonAssistantContent() throws IOException {
        String responseJson = fixture("fixtures/clawgic/provider/openai_judge_markdown_fenced.json");

        OpenAiJudgeResultParser.ParseResult parseResult =
                OpenAiJudgeResultParser.extractJudgeResult(objectMapper, responseJson);

        ObjectNode result = parseResult.judgeResult();
        assertEquals("00000000-0000-0000-0000-000000000902", result.path("winner_id").asText());
        assertEquals(7, result.path("agent_2").path("persona_adherence").asInt());
    }

    @Test
    void repairsPrefixedAndSuffixedAssistantContent() throws IOException {
        String responseJson = fixture("fixtures/clawgic/provider/openai_judge_prefixed_text.json");

        OpenAiJudgeResultParser.ParseResult parseResult =
                OpenAiJudgeResultParser.extractJudgeResult(objectMapper, responseJson);

        ObjectNode result = parseResult.judgeResult();
        assertEquals("00000000-0000-0000-0000-000000000903", result.path("winner_id").asText());
        assertEquals(9, result.path("agent_1").path("rebuttal_strength").asInt());
        assertTrue(parseResult.assistantText().contains("Winner selected"));
    }

    @Test
    void rejectsAssistantContentWithoutJsonObject() throws IOException {
        String responseJson = fixture("fixtures/clawgic/provider/openai_judge_invalid_content.json");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> OpenAiJudgeResultParser.extractJudgeResult(objectMapper, responseJson)
        );

        assertEquals("OpenAI judge response content is not a valid JSON object", ex.getMessage());
    }

    private static String fixture(String resourcePath) throws IOException {
        try (InputStream inputStream = OpenAiJudgeResultParserTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Missing fixture: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
