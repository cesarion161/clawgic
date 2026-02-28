package com.clawgic.clawgic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.clawgic.clawgic.config.ClawgicAgentKeyEncryptionProperties;
import com.clawgic.clawgic.model.ClawgicAgent;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/**
 * Encrypts/decrypts user-provided provider API keys before they are persisted in Clawgic agent records.
 */
@Service
public class ClawgicAgentApiKeyCryptoService {

    private static final String ENVELOPE_VERSION = "v1";
    private static final String ENVELOPE_ALGORITHM = "AES-256-GCM";
    private static final int AES_KEY_BYTES = 32;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final ClawgicAgentKeyEncryptionProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();

    public ClawgicAgentApiKeyCryptoService(ClawgicAgentKeyEncryptionProperties properties) {
        this.properties = properties;
    }

    public EncryptedApiKey encryptForStorage(String plaintextApiKey) {
        try {
            String normalizedApiKey = normalizePlaintextApiKey(plaintextApiKey);
            String keyId = requireActiveKeyId();
            byte[] keyBytes = lookupKeyBytes(keyId);
            byte[] iv = randomIv();

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(storageAad(keyId));

            byte[] ciphertext = cipher.doFinal(normalizedApiKey.getBytes(StandardCharsets.UTF_8));
            String envelopeJson = objectMapper.writeValueAsString(new StorageEnvelope(
                    ENVELOPE_VERSION,
                    ENVELOPE_ALGORITHM,
                    keyId,
                    Base64.getEncoder().encodeToString(iv),
                    Base64.getEncoder().encodeToString(ciphertext)
            ));
            return new EncryptedApiKey(envelopeJson, keyId);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to encrypt Clawgic agent API key for storage", ex);
        }
    }

    public void applyEncryptedApiKey(ClawgicAgent agent, String plaintextApiKey) {
        if (agent == null) {
            throw new IllegalArgumentException("Clawgic agent is required");
        }
        EncryptedApiKey encryptedApiKey = encryptForStorage(plaintextApiKey);
        agent.setApiKeyEncrypted(encryptedApiKey.encryptedValue());
        agent.setApiKeyEncryptionKeyId(encryptedApiKey.keyId());
    }

    public String decryptFromStorage(ClawgicAgent agent) {
        if (agent == null) {
            throw new IllegalArgumentException("Clawgic agent is required");
        }
        if (!StringUtils.hasText(agent.getApiKeyEncrypted())) {
            throw new IllegalArgumentException("Clawgic agent does not have an encrypted API key");
        }
        return decryptFromStorage(agent.getApiKeyEncrypted(), agent.getApiKeyEncryptionKeyId());
    }

    public String decryptFromStorage(String encryptedValue, String keyIdFromColumn) {
        try {
            if (!StringUtils.hasText(encryptedValue)) {
                throw new IllegalArgumentException("Encrypted API key payload is required");
            }

            StorageEnvelope envelope = objectMapper.readValue(encryptedValue, StorageEnvelope.class);
            validateEnvelope(envelope);

            String envelopeKeyId = envelope.kid().trim();
            if (StringUtils.hasText(keyIdFromColumn) && !envelopeKeyId.equals(keyIdFromColumn.trim())) {
                throw new IllegalArgumentException("API key encryption key id mismatch between column and envelope");
            }

            byte[] keyBytes = lookupKeyBytes(envelopeKeyId);
            byte[] iv = decodeBase64("iv", envelope.iv());
            if (iv.length != GCM_IV_BYTES) {
                throw new IllegalArgumentException("API key envelope iv must be 12 bytes");
            }

            byte[] ciphertext = decodeBase64("ciphertext", envelope.ct());

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(storageAad(envelopeKeyId));
            byte[] plaintextBytes = cipher.doFinal(ciphertext);

            String decryptedApiKey = new String(plaintextBytes, StandardCharsets.UTF_8);
            return normalizePlaintextApiKey(decryptedApiKey);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to decrypt Clawgic agent API key from storage", ex);
        }
    }

    public String redactForLogs(String plaintextApiKey) {
        if (!StringUtils.hasText(plaintextApiKey)) {
            return "<empty>";
        }

        String normalizedApiKey = plaintextApiKey.trim();
        if (normalizedApiKey.length() <= 6) {
            return "<redacted:" + normalizedApiKey.length() + ">";
        }

        return normalizedApiKey.substring(0, 4)
                + "..."
                + normalizedApiKey.substring(normalizedApiKey.length() - 2)
                + " (len=" + normalizedApiKey.length() + ")";
    }

    private void validateEnvelope(StorageEnvelope envelope) {
        if (envelope == null) {
            throw new IllegalArgumentException("Encrypted API key payload is invalid");
        }
        if (!ENVELOPE_VERSION.equals(envelope.version())) {
            throw new IllegalArgumentException("Unsupported API key envelope version: " + envelope.version());
        }
        if (!ENVELOPE_ALGORITHM.equals(envelope.alg())) {
            throw new IllegalArgumentException("Unsupported API key envelope algorithm: " + envelope.alg());
        }
        if (!StringUtils.hasText(envelope.kid())) {
            throw new IllegalArgumentException("API key envelope is missing key id");
        }
        if (!StringUtils.hasText(envelope.iv()) || !StringUtils.hasText(envelope.ct())) {
            throw new IllegalArgumentException("API key envelope is missing required ciphertext fields");
        }
    }

    private String normalizePlaintextApiKey(String plaintextApiKey) {
        if (plaintextApiKey == null) {
            throw new IllegalArgumentException("Agent API key is required");
        }

        String normalizedApiKey = plaintextApiKey.trim();
        if (!StringUtils.hasText(normalizedApiKey)) {
            throw new IllegalArgumentException("Agent API key is required");
        }

        int maxLength = properties.getMaxPlaintextLength();
        if (maxLength > 0 && normalizedApiKey.length() > maxLength) {
            throw new IllegalArgumentException("Agent API key exceeds max plaintext length");
        }

        return normalizedApiKey;
    }

    private String requireActiveKeyId() {
        if (!StringUtils.hasText(properties.getActiveKeyId())) {
            throw new IllegalArgumentException("Active Clawgic agent API key encryption key id is not configured");
        }
        return properties.getActiveKeyId().trim();
    }

    private byte[] lookupKeyBytes(String keyId) {
        Map<String, String> configuredKeys = properties.getKeys();
        if (configuredKeys == null || configuredKeys.isEmpty()) {
            throw new IllegalArgumentException("No Clawgic agent API key encryption keys are configured");
        }

        String encodedKey = configuredKeys.get(keyId);
        if (!StringUtils.hasText(encodedKey)) {
            throw new IllegalArgumentException("Clawgic agent API key encryption key is not configured for key id: " + keyId);
        }

        byte[] keyBytes = decodeBase64("encryption key", encodedKey);
        if (keyBytes.length != AES_KEY_BYTES) {
            throw new IllegalArgumentException("Clawgic agent API key encryption key must decode to exactly 32 bytes");
        }
        return keyBytes;
    }

    private byte[] decodeBase64(String fieldName, String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid base64 value for " + fieldName, ex);
        }
    }

    private byte[] storageAad(String keyId) {
        return ("clawgic-agent-api-key|" + ENVELOPE_VERSION + "|" + keyId).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] randomIv() {
        byte[] iv = new byte[GCM_IV_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    public record EncryptedApiKey(String encryptedValue, String keyId) {

        public EncryptedApiKey {
            if (!StringUtils.hasText(encryptedValue)) {
                throw new IllegalArgumentException("Encrypted API key payload is required");
            }
            if (!StringUtils.hasText(keyId)) {
                throw new IllegalArgumentException("Encrypted API key key id is required");
            }
        }

        @Override
        public @NonNull String toString() {
            return "EncryptedApiKey[keyId=" + keyId + ", encryptedValue=<redacted>]";
        }
    }

    private record StorageEnvelope(
            String version,
            String alg,
            String kid,
            String iv,
            String ct
    ) {
    }
}
