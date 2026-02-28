package com.clawgic.controller.dto;

import com.clawgic.model.SubscriptionType;

public record CreateSubscriptionRequest(
        String readerWallet,
        Long amount,
        SubscriptionType type,
        Integer marketId,
        Integer roundId,
        IdReference market,
        IdReference round
) {
    public Integer resolvedMarketId() {
        if (marketId != null) {
            return marketId;
        }
        return market != null ? market.id() : null;
    }

    public Integer resolvedRoundId() {
        if (roundId != null) {
            return roundId;
        }
        return round != null ? round.id() : null;
    }

    public boolean isValid() {
        return isPresent(readerWallet)
                && amount != null
                && amount > 0
                && type != null
                && resolvedMarketId() != null;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    public record IdReference(Integer id) {
    }
}
