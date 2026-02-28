package com.clawgic.controller.dto;

import com.clawgic.model.Market;

public record MarketResponse(
        Integer id,
        String name,
        String submoltId
) {
    public static MarketResponse from(Market market) {
        if (market == null) {
            return null;
        }

        return new MarketResponse(
                market.getId(),
                market.getName(),
                market.getSubmoltId()
        );
    }
}
