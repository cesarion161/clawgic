package com.clawgic.clawgic.service;

import com.clawgic.clawgic.config.ClawgicRuntimeProperties;
import com.clawgic.clawgic.dto.ClawgicHealthResponse;
import com.clawgic.clawgic.model.ClawgicSkeletonStatus;
import org.springframework.stereotype.Service;

/**
 * Temporary Clawgic service stub to establish backend package boundaries.
 */
@Service
public class ClawgicHealthService {

    private final ClawgicRuntimeProperties clawgicRuntimeProperties;

    public ClawgicHealthService(ClawgicRuntimeProperties clawgicRuntimeProperties) {
        this.clawgicRuntimeProperties = clawgicRuntimeProperties;
    }

    public ClawgicHealthResponse health() {
        return new ClawgicHealthResponse(
                "clawgic",
                ClawgicSkeletonStatus.STUB,
                clawgicRuntimeProperties.isEnabled(),
                clawgicRuntimeProperties.isMockProvider(),
                clawgicRuntimeProperties.isMockJudge());
    }
}
