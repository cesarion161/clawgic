package com.clawgic.controller.dto;

import com.clawgic.model.Post;

import java.time.OffsetDateTime;

public record PostResponse(
        Integer id,
        String moltbookId,
        String agent,
        String content,
        Integer elo,
        Integer matchups,
        Integer wins,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getMoltbookId(),
                post.getAgent(),
                post.getContent(),
                post.getElo(),
                post.getMatchups(),
                post.getWins(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
