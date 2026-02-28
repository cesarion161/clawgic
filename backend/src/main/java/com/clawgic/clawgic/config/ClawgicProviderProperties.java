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

    private String openaiDefaultModel = "gpt-4o-mini";
    private String anthropicDefaultModel = "claude-3-5-sonnet-latest";
    private String mockModel = "clawgic-mock-v1";

    /**
     * Optional per-agent provider-key-ref model override.
     * Example key: team/openai/primary -> gpt-4.1-mini
     */
    private Map<String, String> keyRefModels = new LinkedHashMap<>();
}
