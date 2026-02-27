package com.moltrank.clawgic.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

public final class DebateTranscriptJsonCodec {

    private static final String FIELD_ROLE = "role";
    private static final String FIELD_PHASE = "phase";
    private static final String FIELD_CONTENT = "content";

    private DebateTranscriptJsonCodec() {
    }

    public static ArrayNode emptyTranscript() {
        return JsonNodeFactory.instance.arrayNode();
    }

    public static ArrayNode toJson(List<DebateTranscriptMessage> messages) {
        ArrayNode transcript = JsonNodeFactory.instance.arrayNode();
        for (DebateTranscriptMessage message : messages) {
            transcript.add(toNode(message));
        }
        return transcript;
    }

    public static ArrayNode append(JsonNode transcriptJson, DebateTranscriptMessage message) {
        ArrayNode transcript = copyAsArray(transcriptJson);
        transcript.add(toNode(message));
        return transcript;
    }

    public static List<DebateTranscriptMessage> fromJson(JsonNode transcriptJson) {
        if (transcriptJson == null || transcriptJson.isNull()) {
            return List.of();
        }
        if (!transcriptJson.isArray()) {
            throw new IllegalArgumentException("Transcript JSON must be an array");
        }

        List<DebateTranscriptMessage> messages = new ArrayList<>();
        int index = 0;
        for (JsonNode node : transcriptJson) {
            if (!node.isObject()) {
                throw new IllegalArgumentException("Transcript entry at index " + index + " must be an object");
            }
            String roleValue = requireTextField(node, FIELD_ROLE, index);
            String phaseValue = requireTextField(node, FIELD_PHASE, index);
            String contentValue = requireTextField(node, FIELD_CONTENT, index);

            DebateTranscriptRole role;
            try {
                role = DebateTranscriptRole.fromWireValue(roleValue);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Transcript entry at index " + index + " has invalid role: " + roleValue,
                        ex
                );
            }

            DebatePhase phase;
            try {
                phase = DebatePhase.valueOf(phaseValue);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Transcript entry at index " + index + " has invalid phase: " + phaseValue,
                        ex
                );
            }

            messages.add(new DebateTranscriptMessage(role, phase, contentValue));
            index++;
        }

        return List.copyOf(messages);
    }

    private static String requireTextField(JsonNode node, String fieldName, int index) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || !fieldNode.isTextual()) {
            throw new IllegalArgumentException(
                    "Transcript entry at index " + index + " is missing textual field '" + fieldName + "'"
            );
        }
        return fieldNode.textValue();
    }

    private static ObjectNode toNode(DebateTranscriptMessage message) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put(FIELD_ROLE, message.role().wireValue());
        node.put(FIELD_PHASE, message.phase().name());
        node.put(FIELD_CONTENT, message.content());
        return node;
    }

    private static ArrayNode copyAsArray(JsonNode transcriptJson) {
        if (transcriptJson == null || transcriptJson.isNull()) {
            return emptyTranscript();
        }
        if (!transcriptJson.isArray()) {
            throw new IllegalArgumentException("Transcript JSON must be an array");
        }
        return ((ArrayNode) transcriptJson).deepCopy();
    }
}
