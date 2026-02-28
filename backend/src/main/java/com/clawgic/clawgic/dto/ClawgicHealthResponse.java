package com.clawgic.clawgic.dto;

import com.clawgic.clawgic.model.ClawgicSkeletonStatus;

/**
 * Minimal response DTO used while the Clawgic API surface is being built out.
 */
public record ClawgicHealthResponse(
        String service,
        ClawgicSkeletonStatus status,
        boolean clawgicEnabled,
        boolean mockProvider,
        boolean mockJudge) {
}
