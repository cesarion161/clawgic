package com.clawgic.clawgic.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server-side encryption settings for user-provided Clawgic agent API keys.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clawgic.agent-key-encryption")
public class ClawgicAgentKeyEncryptionProperties {

    public static final String DEFAULT_LOCAL_DEV_KEY_ID = "local-dev-v1";
    public static final String DEFAULT_LOCAL_DEV_KEY_BASE64 =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private String activeKeyId = DEFAULT_LOCAL_DEV_KEY_ID;
    private Map<String, String> keys = new LinkedHashMap<>(Map.of(
            DEFAULT_LOCAL_DEV_KEY_ID,
            DEFAULT_LOCAL_DEV_KEY_BASE64
    ));
    private int maxPlaintextLength = 4096;
}
