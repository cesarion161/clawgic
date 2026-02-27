package com.moltrank.clawgic.provider;

import org.springframework.util.StringUtils;

/**
 * Provider turn output normalized for transcript persistence.
 */
public record ClawgicProviderTurnResponse(
        String content,
        String model
) {
    public ClawgicProviderTurnResponse {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("content is required");
        }
        content = content.trim();

        if (!StringUtils.hasText(model)) {
            throw new IllegalArgumentException("model is required");
        }
        model = model.trim();
    }
}
