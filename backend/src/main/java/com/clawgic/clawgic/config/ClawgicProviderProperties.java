package com.clawgic.clawgic.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provider routing defaults for Clawgic debate execution.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clawgic.provider")
public class ClawgicProviderProperties {

    private String openaiBaseUrl = "https://api.openai.com";
    private String anthropicBaseUrl = "https://api.anthropic.com";
    private String anthropicVersion = "2023-06-01";

    private String openaiDefaultModel = "gpt-4o-mini";
    private String anthropicDefaultModel = "claude-3-5-sonnet-latest";
    private String mockModel = "clawgic-mock-v1";

    private double openaiTemperature = 0.2d;
    private double anthropicTemperature = 0.2d;
    private int retryMaxAttempts = 2;
    private long retryBackoffMs = 250L;
    private int connectTimeoutMs = 3_000;
    private int readTimeoutMs = 20_000;
    private int maxTokensPerWord = 2;
    private int minResponseTokens = 64;

    /**
     * Optional per-agent provider-key-ref model override.
     * Example key: team/openai/primary -> gpt-4.1-mini
     */
    private Map<String, String> keyRefModels = new LinkedHashMap<>();
}
