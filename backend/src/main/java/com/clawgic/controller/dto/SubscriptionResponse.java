package com.clawgic.controller.dto;

import com.clawgic.model.Subscription;
import com.clawgic.model.SubscriptionType;
import com.clawgic.service.SubscriptionService;

import java.time.OffsetDateTime;

public record SubscriptionResponse(
        Integer id,
        String readerWallet,
        Integer marketId,
        Integer roundId,
        Long amount,
        SubscriptionType type,
        OffsetDateTime subscribedAt,
        OffsetDateTime expiresAt,
        Integer marketSubscribers,
        Long marketSubscriptionRevenue,
        Long marketRevenueDelta,
        Long poolContribution,
        Long poolBalance
) {
    public static SubscriptionResponse from(SubscriptionService.SubscriptionCreationResult result) {
        Subscription subscription = result.subscription();
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getReaderWallet(),
                subscription.getMarket().getId(),
                subscription.getRound() != null ? subscription.getRound().getId() : null,
                subscription.getAmount(),
                subscription.getType(),
                subscription.getSubscribedAt(),
                subscription.getExpiresAt(),
                result.market().getSubscribers(),
                result.market().getSubscriptionRevenue(),
                result.marketRevenueDelta(),
                result.poolContribution(),
                result.poolBalance()
        );
    }
}
