package com.moltrank.clawgic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moltrank.clawgic.web.X402PaymentRequestException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class X402PaymentHeaderParser {

    private static final int MAX_FIELD_LENGTH = 128;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ParsedX402PaymentHeader parse(String paymentHeaderValue) {
        if (paymentHeaderValue == null || paymentHeaderValue.isBlank()) {
            throw X402PaymentRequestException.malformedHeader("X-PAYMENT header is required");
        }

        JsonNode root = parseJsonObject(paymentHeaderValue);
        JsonNode payload = root.path("payload");
        if (!payload.isMissingNode() && !payload.isObject()) {
            throw X402PaymentRequestException.malformedHeader("X-PAYMENT payload must be a JSON object");
        }

        String requestNonce = readRequiredString(root, "requestNonce");
        String idempotencyKey = firstNonBlankString(root, payload, "idempotencyKey");
        if (idempotencyKey == null) {
            throw X402PaymentRequestException.malformedHeader("X-PAYMENT.idempotencyKey is required");
        }
        validateLength("idempotencyKey", idempotencyKey);

        String authorizationNonce = firstNonBlankString(root, payload, "authorizationNonce");
        if (authorizationNonce != null) {
            validateLength("authorizationNonce", authorizationNonce);
        }

        BigDecimal amountUsdc = readOptionalAmount(root, payload);
        Long chainId = readOptionalLong(root, payload, "chainId");
        String recipient = firstNonBlankString(root, payload, "recipient");

        return new ParsedX402PaymentHeader(
                root,
                requestNonce,
                idempotencyKey,
                authorizationNonce,
                amountUsdc,
                chainId,
                recipient
        );
    }

    private JsonNode parseJsonObject(String paymentHeaderValue) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(paymentHeaderValue);
            if (!root.isObject()) {
                throw X402PaymentRequestException.malformedHeader("X-PAYMENT header must be a JSON object");
            }
            return root;
        } catch (JsonProcessingException ex) {
            throw X402PaymentRequestException.malformedHeader("X-PAYMENT header must be valid JSON");
        }
    }

    private String readRequiredString(JsonNode root, String fieldName) {
        String value = readOptionalString(root, fieldName);
        if (value == null) {
            throw X402PaymentRequestException.malformedHeader("X-PAYMENT." + fieldName + " is required");
        }
        validateLength(fieldName, value);
        return value;
    }

    private String firstNonBlankString(JsonNode topLevel, JsonNode nested, String fieldName) {
        String topLevelValue = readOptionalString(topLevel, fieldName);
        if (topLevelValue != null) {
            return topLevelValue;
        }
        return readOptionalString(nested, fieldName);
    }

    private String readOptionalString(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }
        if (!fieldNode.isTextual()) {
            throw X402PaymentRequestException.malformedHeader("X-PAYMENT." + fieldName + " must be a string");
        }
        String value = fieldNode.asText().trim();
        if (value.isEmpty()) {
            return null;
        }
        return value;
    }

    private BigDecimal readOptionalAmount(JsonNode topLevel, JsonNode nested) {
        JsonNode amountNode = firstPresent(topLevel, nested, "amountUsdc", "amount");
        if (amountNode == null || amountNode.isNull()) {
            return null;
        }

        String rawAmount;
        if (amountNode.isNumber() || amountNode.isTextual()) {
            rawAmount = amountNode.asText();
        } else {
            throw X402PaymentRequestException.malformedHeader("X-PAYMENT.amountUsdc must be numeric");
        }

        try {
            BigDecimal parsed = new BigDecimal(rawAmount);
            if (parsed.signum() < 0) {
                throw X402PaymentRequestException.malformedHeader("X-PAYMENT.amountUsdc must be non-negative");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw X402PaymentRequestException.malformedHeader("X-PAYMENT.amountUsdc must be numeric");
        }
    }

    private Long readOptionalLong(JsonNode topLevel, JsonNode nested, String fieldName) {
        JsonNode fieldNode = firstPresent(topLevel, nested, fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        if (fieldNode.isIntegralNumber()) {
            return fieldNode.asLong();
        }
        if (fieldNode.isTextual()) {
            try {
                return Long.parseLong(fieldNode.asText().trim());
            } catch (NumberFormatException ex) {
                throw X402PaymentRequestException.malformedHeader("X-PAYMENT." + fieldName + " must be an integer");
            }
        }
        throw X402PaymentRequestException.malformedHeader("X-PAYMENT." + fieldName + " must be an integer");
    }

    private JsonNode firstPresent(JsonNode topLevel, JsonNode nested, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode topLevelField = topLevel.path(fieldName);
            if (!topLevelField.isMissingNode()) {
                return topLevelField;
            }
            JsonNode nestedField = nested.path(fieldName);
            if (!nestedField.isMissingNode()) {
                return nestedField;
            }
        }
        return null;
    }

    private void validateLength(String fieldName, String value) {
        if (value.length() > MAX_FIELD_LENGTH) {
            throw X402PaymentRequestException.malformedHeader(
                    "X-PAYMENT." + fieldName + " exceeds max length of " + MAX_FIELD_LENGTH
            );
        }
    }

    public record ParsedX402PaymentHeader(
            JsonNode rawJson,
            String requestNonce,
            String idempotencyKey,
            String authorizationNonce,
            BigDecimal amountUsdc,
            Long chainId,
            String recipient
    ) {
    }
}
