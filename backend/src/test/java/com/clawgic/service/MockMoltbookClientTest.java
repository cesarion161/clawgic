package com.clawgic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MockMoltbookClientTest {

    private MockMoltbookClient client;

    @BeforeEach
    void setUp() {
        client = new MockMoltbookClient();
    }

    @Test
    void fetchPosts_knownSubmolt_returnsSubmoltSpecificPosts() {
        List<MoltbookClient.MoltbookPost> techPosts = client.fetchPosts("tech", 100);
        List<MoltbookClient.MoltbookPost> sciencePosts = client.fetchPosts("science", 100);

        assertFalse(techPosts.isEmpty());
        assertFalse(sciencePosts.isEmpty());

        Set<String> techIds = new HashSet<>();
        techPosts.forEach(p -> techIds.add(p.moltbookId()));
        Set<String> scienceIds = new HashSet<>();
        sciencePosts.forEach(p -> scienceIds.add(p.moltbookId()));

        // Tech and science pools should have distinct posts (some may overlap in defaults)
        assertNotEquals(techIds, scienceIds, "Different submolts should return different post sets");
    }

    @Test
    void fetchPosts_unknownSubmolt_returnsDefaultPosts() {
        List<MoltbookClient.MoltbookPost> posts = client.fetchPosts("unknown_submolt", 10);
        assertFalse(posts.isEmpty());
        assertEquals(10, posts.size());
    }

    @Test
    void fetchPosts_respectsLimit() {
        List<MoltbookClient.MoltbookPost> posts = client.fetchPosts("tech", 3);
        assertEquals(3, posts.size());
    }

    @Test
    void fetchPosts_limitExceedsPool_returnsAll() {
        List<MoltbookClient.MoltbookPost> posts = client.fetchPosts("science", 1000);
        assertEquals(5, posts.size()); // science pool has 5 posts
    }

    @Test
    void fetchPosts_shufflesOrder() {
        // Run multiple fetches and check that order varies
        boolean foundDifferentOrder = false;
        List<String> firstOrder = client.fetchPosts("tech", 7).stream()
            .map(MoltbookClient.MoltbookPost::moltbookId).toList();

        for (int i = 0; i < 20; i++) {
            List<String> order = client.fetchPosts("tech", 7).stream()
                .map(MoltbookClient.MoltbookPost::moltbookId).toList();
            if (!order.equals(firstOrder)) {
                foundDifferentOrder = true;
                break;
            }
        }
        assertTrue(foundDifferentOrder, "Posts should be shuffled across calls");
    }

    @Test
    void fetchPosts_variesEngagementMetrics() {
        // Fetch same submolt multiple times and check that engagement varies
        boolean foundVariation = false;
        int firstLikes = client.fetchPosts("tech", 1).get(0).likes();

        // With shuffling, the first post varies too - check across all returned posts
        Set<Integer> seenLikes = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            client.fetchPosts("tech", 7).forEach(p -> seenLikes.add(p.likes()));
        }
        // With 7 posts * 20 fetches and jitter, we should see more than 7 unique like values
        assertTrue(seenLikes.size() > 7, "Engagement metrics should vary across calls");
    }

    @Test
    void fetchPosts_allPostsHaveValidFields() {
        for (String submolt : List.of("tech", "science", "culture", "unknown")) {
            List<MoltbookClient.MoltbookPost> posts = client.fetchPosts(submolt, 100);
            for (MoltbookClient.MoltbookPost post : posts) {
                assertNotNull(post.moltbookId(), "moltbookId must not be null");
                assertNotNull(post.agent(), "agent must not be null");
                assertNotNull(post.content(), "content must not be null");
                assertFalse(post.content().isEmpty(), "content must not be empty");
                assertTrue(post.likes() >= 0, "likes must be non-negative");
                assertTrue(post.shares() >= 0, "shares must be non-negative");
                assertTrue(post.replies() >= 0, "replies must be non-negative");
            }
        }
    }

    @Test
    void fetchPosts_cultureSubmolt_returnsCulturePosts() {
        List<MoltbookClient.MoltbookPost> posts = client.fetchPosts("culture", 100);
        assertEquals(5, posts.size());
        Set<String> ids = new HashSet<>();
        posts.forEach(p -> ids.add(p.moltbookId()));
        assertTrue(ids.contains("mb-015") || ids.contains("mb-016") || ids.contains("mb-017"),
            "Culture submolt should contain culture-specific posts");
    }
}
