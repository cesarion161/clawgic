package com.moltrank.clawgic.model;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DebateTranscriptJsonCodecTest {

    @Test
    void serializesAndDeserializesTranscriptMessagesInOrder() {
        List<DebateTranscriptMessage> expected = List.of(
                new DebateTranscriptMessage(
                        DebateTranscriptRole.SYSTEM,
                        DebatePhase.THESIS_DISCOVERY,
                        "Provide concise arguments."
                ),
                new DebateTranscriptMessage(
                        DebateTranscriptRole.AGENT_1,
                        DebatePhase.ARGUMENTATION,
                        "Deterministic mocks improve repeatability."
                ),
                new DebateTranscriptMessage(
                        DebateTranscriptRole.AGENT_2,
                        DebatePhase.COUNTER_ARGUMENTATION,
                        "Live calls reveal production failure modes."
                ),
                new DebateTranscriptMessage(
                        DebateTranscriptRole.AGENT_1,
                        DebatePhase.CONCLUSION,
                        "Mock coverage should be mandatory for MVP reliability."
                )
        );

        var transcriptJson = DebateTranscriptJsonCodec.toJson(expected);
        List<DebateTranscriptMessage> actual = DebateTranscriptJsonCodec.fromJson(transcriptJson);

        assertEquals(expected, actual);
    }

    @Test
    void appendAddsMessageWithoutMutatingOriginalArray() {
        var existing = DebateTranscriptJsonCodec.toJson(List.of(
                new DebateTranscriptMessage(
                        DebateTranscriptRole.SYSTEM,
                        DebatePhase.THESIS_DISCOVERY,
                        "Stay precise."
                )
        ));
        DebateTranscriptMessage appendedMessage = new DebateTranscriptMessage(
                DebateTranscriptRole.AGENT_1,
                DebatePhase.ARGUMENTATION,
                "Structured phases improve judging quality."
        );

        var appended = DebateTranscriptJsonCodec.append(existing, appendedMessage);

        assertEquals(1, existing.size());
        assertEquals(2, appended.size());
        assertEquals("agent1", appended.get(1).get("role").asText());
        assertEquals("ARGUMENTATION", appended.get(1).get("phase").asText());
    }

    @Test
    void fromJsonRejectsInvalidPayloadShapes() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> DebateTranscriptJsonCodec.fromJson(JsonNodeFactory.instance.objectNode())
        );

        assertEquals("Transcript JSON must be an array", ex.getMessage());
    }

    @Test
    void fromJsonRejectsInvalidRoleValues() {
        var invalid = JsonNodeFactory.instance.arrayNode();
        var node = invalid.addObject();
        node.put("role", "unknown");
        node.put("phase", "THESIS_DISCOVERY");
        node.put("content", "Invalid role test");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> DebateTranscriptJsonCodec.fromJson(invalid)
        );

        assertEquals(
                "Transcript entry at index 0 has invalid role: unknown",
                ex.getMessage()
        );
    }
}
