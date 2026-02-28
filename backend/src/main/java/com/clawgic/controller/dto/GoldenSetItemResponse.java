package com.clawgic.controller.dto;

import com.clawgic.model.GoldenSetItem;
import com.clawgic.model.PairWinner;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record GoldenSetItemResponse(
        Integer id,
        PostResponse postA,
        PostResponse postB,
        PairWinner correctAnswer,
        BigDecimal confidence,
        String source,
        OffsetDateTime createdAt
) {
    public static GoldenSetItemResponse from(GoldenSetItem item) {
        return new GoldenSetItemResponse(
                item.getId(),
                PostResponse.from(item.getPostA()),
                PostResponse.from(item.getPostB()),
                item.getCorrectAnswer(),
                item.getConfidence(),
                item.getSource(),
                item.getCreatedAt()
        );
    }
}
