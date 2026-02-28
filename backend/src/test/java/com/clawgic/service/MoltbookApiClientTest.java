package com.clawgic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MoltbookApiClient against the real Moltbook API.
 *
 * These tests only run when MOLTBOOK_API_URL is set, e.g.:
 *   MOLTBOOK_API_URL=https://api.moltbook.com ./gradlew test
 */
@EnabledIfEnvironmentVariable(named = "MOLTBOOK_API_URL", matches = ".+")
class MoltbookApiClientTest {

    private MoltbookApiClient client;

    @BeforeEach
    void setUp() {
        MoltbookApiProperties props = new MoltbookApiProperties();
        props.setApiUrl(System.getenv("MOLTBOOK_API_URL"));
        client = new MoltbookApiClient(props);
    }

    @Test
    void fetchPosts_returnsNonEmptyList() {
        List<MoltbookClient.MoltbookPost> posts = client.fetchPosts("tech", 10);
        assertFalse(posts.isEmpty(), "API should return posts for known submolt");
    }

    @Test
    void fetchPosts_respectsLimit() {
        List<MoltbookClient.MoltbookPost> posts = client.fetchPosts("tech", 3);
        assertTrue(posts.size() <= 3, "Should return at most 'limit' posts");
    }

    @Test
    void fetchPosts_postsHaveRequiredFields() {
        List<MoltbookClient.MoltbookPost> posts = client.fetchPosts("tech", 10);
        for (MoltbookClient.MoltbookPost post : posts) {
            assertNotNull(post.moltbookId(), "moltbookId must not be null");
            assertFalse(post.moltbookId().isBlank(), "moltbookId must not be blank");
            assertNotNull(post.agent(), "agent must not be null");
            assertFalse(post.agent().isBlank(), "agent must not be blank");
            assertNotNull(post.content(), "content must not be null");
            assertFalse(post.content().isBlank(), "content must not be blank");
            assertTrue(post.likes() >= 0, "likes must be non-negative");
            assertTrue(post.shares() >= 0, "shares must be non-negative");
            assertTrue(post.replies() >= 0, "replies must be non-negative");
        }
    }

    @Test
    void fetchPosts_returnsUniquePosts() {
        List<MoltbookClient.MoltbookPost> posts = client.fetchPosts("tech", 10);
        Set<String> ids = new HashSet<>();
        for (MoltbookClient.MoltbookPost post : posts) {
            assertTrue(ids.add(post.moltbookId()),
                    "Duplicate moltbookId found: " + post.moltbookId());
        }
    }

    @Test
    void fetchPosts_unknownSubmolt_returnsGracefully() {
        List<MoltbookClient.MoltbookPost> posts = client.fetchPosts("nonexistent_submolt_xyz", 10);
        assertNotNull(posts, "Should return a list (possibly empty) for unknown submolt");
    }

    @Test
    void fetchPosts_differentSubmolts_returnDifferentPosts() {
        List<MoltbookClient.MoltbookPost> techPosts = client.fetchPosts("tech", 10);
        List<MoltbookClient.MoltbookPost> sciencePosts = client.fetchPosts("science", 10);

        // At least one should be non-empty for this test to be meaningful
        if (techPosts.isEmpty() && sciencePosts.isEmpty()) {
            return; // API may not have data yet; skip assertion
        }

        Set<String> techIds = new HashSet<>();
        techPosts.forEach(p -> techIds.add(p.moltbookId()));
        Set<String> scienceIds = new HashSet<>();
        sciencePosts.forEach(p -> scienceIds.add(p.moltbookId()));

        assertNotEquals(techIds, scienceIds,
                "Different submolts should return different post sets");
    }

    @Test
    void fetchPosts_zeroLimit_returnsEmptyOrHandlesGracefully() {
        List<MoltbookClient.MoltbookPost> posts = client.fetchPosts("tech", 0);
        assertNotNull(posts, "Should handle zero limit gracefully");
        assertTrue(posts.size() <= 0, "Zero limit should return no posts");
    }
}
