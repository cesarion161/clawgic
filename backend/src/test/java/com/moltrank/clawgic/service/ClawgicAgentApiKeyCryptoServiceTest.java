package com.moltrank.clawgic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.moltrank.clawgic.config.ClawgicAgentKeyEncryptionProperties;
import com.moltrank.clawgic.model.ClawgicAgent;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClawgicAgentApiKeyCryptoServiceTest {

    private static final String KEY_V1_BASE64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String KEY_V2_BASE64 = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void encryptDecryptRoundTripAndEntityStorageContract() {
        ClawgicAgentApiKeyCryptoService service = new ClawgicAgentApiKeyCryptoService(configuredProperties());

        String plaintextApiKey = "  sk-live-1234567890abcdef  ";
        ClawgicAgentApiKeyCryptoService.EncryptedApiKey encryptedApiKey = service.encryptForStorage(plaintextApiKey);

        assertEquals("v1", encryptedApiKey.keyId());
        assertNotNull(encryptedApiKey.encryptedValue());
        assertFalse(encryptedApiKey.encryptedValue().contains("sk-live-1234567890abcdef"));
        assertTrue(encryptedApiKey.encryptedValue().contains("\"kid\":\"v1\""));
        assertTrue(encryptedApiKey.toString().contains("<redacted>"));
        assertFalse(encryptedApiKey.toString().contains("sk-live-1234567890abcdef"));
        assertTrue(service.redactForLogs(plaintextApiKey).contains("len="));
        assertFalse(service.redactForLogs(plaintextApiKey).contains("sk-live-1234567890abcdef"));

        String decryptedApiKey = service.decryptFromStorage(encryptedApiKey.encryptedValue(), encryptedApiKey.keyId());
        assertEquals("sk-live-1234567890abcdef", decryptedApiKey);

        ClawgicAgent agent = new ClawgicAgent();
        service.applyEncryptedApiKey(agent, "sk-test-abcdef0123456789");
        assertNotNull(agent.getApiKeyEncrypted());
        assertEquals("v1", agent.getApiKeyEncryptionKeyId());
        assertEquals("sk-test-abcdef0123456789", service.decryptFromStorage(agent));
    }

    @Test
    void decryptRejectsTamperedCiphertextEnvelope() throws Exception {
        ClawgicAgentApiKeyCryptoService service = new ClawgicAgentApiKeyCryptoService(configuredProperties());

        ClawgicAgentApiKeyCryptoService.EncryptedApiKey encryptedApiKey = service.encryptForStorage("sk-live-tamper-test");
        JsonNode node = objectMapper.readTree(encryptedApiKey.encryptedValue());
        ObjectNode tampered = (ObjectNode) node;
        String originalCiphertext = tampered.get("ct").asText();
        tampered.put("ct", originalCiphertext.substring(0, originalCiphertext.length() - 2) + "AA");
        String tamperedEnvelope = objectMapper.writeValueAsString(tampered);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.decryptFromStorage(tamperedEnvelope, encryptedApiKey.keyId())
        );
        assertTrue(exception.getMessage().contains("Unable to decrypt")
                || exception.getMessage().contains("Invalid base64"));
    }

    @Test
    void decryptRejectsMismatchedKeyIdHint() {
        ClawgicAgentApiKeyCryptoService service = new ClawgicAgentApiKeyCryptoService(configuredProperties());
        ClawgicAgentApiKeyCryptoService.EncryptedApiKey encryptedApiKey = service.encryptForStorage("sk-live-kid-test");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.decryptFromStorage(encryptedApiKey.encryptedValue(), "v2")
        );
        assertTrue(exception.getMessage().contains("mismatch"));
    }

    @Test
    void encryptValidatesRequiredAndMaxLengthPlaintext() {
        ClawgicAgentKeyEncryptionProperties properties = configuredProperties();
        properties.setMaxPlaintextLength(10);
        ClawgicAgentApiKeyCryptoService service = new ClawgicAgentApiKeyCryptoService(properties);

        IllegalArgumentException blankException = assertThrows(
                IllegalArgumentException.class,
                () -> service.encryptForStorage("   ")
        );
        assertEquals("Agent API key is required", blankException.getMessage());

        IllegalArgumentException tooLongException = assertThrows(
                IllegalArgumentException.class,
                () -> service.encryptForStorage("12345678901")
        );
        assertEquals("Agent API key exceeds max plaintext length", tooLongException.getMessage());
    }

    @Test
    void encryptionUsesRandomIvForDistinctCiphertext() {
        ClawgicAgentApiKeyCryptoService service = new ClawgicAgentApiKeyCryptoService(configuredProperties());

        ClawgicAgentApiKeyCryptoService.EncryptedApiKey encryptedOne = service.encryptForStorage("sk-repeatable-input");
        ClawgicAgentApiKeyCryptoService.EncryptedApiKey encryptedTwo = service.encryptForStorage("sk-repeatable-input");

        assertNotEquals(encryptedOne.encryptedValue(), encryptedTwo.encryptedValue());
        assertEquals("sk-repeatable-input", service.decryptFromStorage(encryptedOne.encryptedValue(), encryptedOne.keyId()));
        assertEquals("sk-repeatable-input", service.decryptFromStorage(encryptedTwo.encryptedValue(), encryptedTwo.keyId()));
    }

    private static ClawgicAgentKeyEncryptionProperties configuredProperties() {
        ClawgicAgentKeyEncryptionProperties properties = new ClawgicAgentKeyEncryptionProperties();
        properties.setActiveKeyId("v1");
        properties.setKeys(new LinkedHashMap<>(Map.of(
                "v1", KEY_V1_BASE64,
                "v2", KEY_V2_BASE64
        )));
        properties.setMaxPlaintextLength(4096);
        return properties;
    }
}
