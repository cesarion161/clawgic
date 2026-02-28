package com.clawgic.clawgic.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.clawgic.clawgic.model.ClawgicPaymentAuthorizationStatus;
import com.clawgic.clawgic.model.ClawgicStakingLedgerStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class ClawgicPaymentResponses {

    private ClawgicPaymentResponses() {
    }

    public record PaymentAuthorizationSummary(
            UUID paymentAuthorizationId,
            UUID tournamentId,
            UUID entryId,
            UUID agentId,
            String walletAddress,
            String requestNonce,
            String idempotencyKey,
            String authorizationNonce,
            ClawgicPaymentAuthorizationStatus status,
            BigDecimal amountAuthorizedUsdc,
            Long chainId,
            String recipientWalletAddress,
            OffsetDateTime challengeExpiresAt,
            OffsetDateTime receivedAt,
            OffsetDateTime verifiedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record PaymentAuthorizationDetail(
            UUID paymentAuthorizationId,
            UUID tournamentId,
            UUID entryId,
            UUID agentId,
            String walletAddress,
            String requestNonce,
            String idempotencyKey,
            String authorizationNonce,
            ClawgicPaymentAuthorizationStatus status,
            JsonNode paymentHeaderJson,
            BigDecimal amountAuthorizedUsdc,
            Long chainId,
            String recipientWalletAddress,
            String failureReason,
            OffsetDateTime challengeExpiresAt,
            OffsetDateTime receivedAt,
            OffsetDateTime verifiedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record StakingLedgerSummary(
            UUID stakeId,
            UUID tournamentId,
            UUID entryId,
            UUID paymentAuthorizationId,
            UUID agentId,
            String walletAddress,
            BigDecimal amountStaked,
            BigDecimal judgeFeeDeducted,
            BigDecimal systemRetention,
            BigDecimal rewardPayout,
            ClawgicStakingLedgerStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record StakingLedgerDetail(
            UUID stakeId,
            UUID tournamentId,
            UUID entryId,
            UUID paymentAuthorizationId,
            UUID agentId,
            String walletAddress,
            BigDecimal amountStaked,
            BigDecimal judgeFeeDeducted,
            BigDecimal systemRetention,
            BigDecimal rewardPayout,
            ClawgicStakingLedgerStatus status,
            String settlementNote,
            OffsetDateTime authorizedAt,
            OffsetDateTime enteredAt,
            OffsetDateTime lockedAt,
            OffsetDateTime forfeitedAt,
            OffsetDateTime settledAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }
}
