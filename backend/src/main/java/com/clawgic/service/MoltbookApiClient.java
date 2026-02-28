package com.clawgic.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Real Moltbook API client that fetches posts from api.moltbook.com.
 * Activated when clawgic.ingestion.api-url is configured.
 */
@Component
@ConditionalOnProperty(name = "clawgic.ingestion.enabled", havingValue = "true")
@ConditionalOnProperty(name = "clawgic.ingestion.api-url")
public class MoltbookApiClient implements MoltbookClient {

    private static final Logger log = LoggerFactory.getLogger(MoltbookApiClient.class);

    private final RestClient restClient;

    public MoltbookApiClient(MoltbookApiProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getApiUrl())
                .build();
        log.info("MoltbookApiClient initialized with base URL: {}", properties.getApiUrl());
    }

    @Override
    public List<MoltbookPost> fetchPosts(String submoltId, int limit) {
        log.debug("Fetching posts from Moltbook API: submolt={}, limit={}", submoltId, limit);

        ApiResponse response = restClient.get()
                .uri("/v1/submolts/{submoltId}/posts?limit={limit}", submoltId, limit)
                .retrieve()
                .body(ApiResponse.class);

        if (response == null || response.posts() == null) {
            log.warn("Empty response from Moltbook API for submolt: {}", submoltId);
            return List.of();
        }

        List<MoltbookPost> posts = response.posts().stream()
                .map(ApiPost::toMoltbookPost)
                .toList();

        log.debug("Fetched {} posts from Moltbook API for submolt: {}", posts.size(), submoltId);
        return posts;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApiResponse(
            @JsonProperty("posts") List<ApiPost> posts
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApiPost(
            @JsonProperty("id") String id,
            @JsonProperty("agent") String agent,
            @JsonProperty("content") String content,
            @JsonProperty("likes") int likes,
            @JsonProperty("shares") int shares,
            @JsonProperty("replies") int replies
    ) {
        MoltbookPost toMoltbookPost() {
            return new MoltbookPost(id, agent, content, likes, shares, replies);
        }
    }
}
