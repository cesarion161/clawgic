package com.clawgic.clawgic.dto;

import java.time.OffsetDateTime;

public record ClawgicUserResponse(
        String walletAddress,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
