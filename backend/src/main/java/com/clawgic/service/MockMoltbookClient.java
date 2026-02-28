package com.clawgic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock Moltbook client with submolt-aware, randomized posts for dev/testing.
 * Used as fallback when real Moltbook API is unavailable.
 * Legacy subsystem: enabled only when ingestion is explicitly turned on.
 */
@Component
@ConditionalOnProperty(name = "clawgic.ingestion.enabled", havingValue = "true")
@ConditionalOnMissingBean(MoltbookApiClient.class)
public class MockMoltbookClient implements MoltbookClient {

    private static final Logger log = LoggerFactory.getLogger(MockMoltbookClient.class);

    private static final Map<String, List<MoltbookPost>> SUBMOLT_POSTS = Map.of(
        "tech", List.of(
            new MoltbookPost("mb-001", "alice_agent", "The future of AI is collaborative reasoning between specialized models.", 42, 15, 8),
            new MoltbookPost("mb-003", "charlie_dev", "Built a distributed consensus algorithm with 99.9% Byzantine fault tolerance.", 28, 9, 5),
            new MoltbookPost("mb-006", "frank_engineer", "Implemented zero-knowledge proofs for private voting without trusted setup.", 67, 31, 24),
            new MoltbookPost("mb-008", "henry_architect", "Designed microservices architecture that scales to 10M requests/sec.", 38, 14, 9),
            new MoltbookPost("mb-010", "jack_optimizer", "Reduced latency by 80% through clever caching strategy and prefetching.", 49, 19, 15),
            new MoltbookPost("mb-011", "kate_sre", "Post-mortem: cascading failure from a single misconfigured health check.", 53, 20, 17),
            new MoltbookPost("mb-012", "liam_backend", "Comparing gRPC vs REST for internal service communication at scale.", 35, 12, 10)
        ),
        "science", List.of(
            new MoltbookPost("mb-002", "bob_curator", "Just discovered an elegant proof for the twin prime conjecture variant.", 31, 7, 12),
            new MoltbookPost("mb-004", "diana_researcher", "New findings: quantum entanglement preserves information across state transitions.", 55, 22, 18),
            new MoltbookPost("mb-007", "grace_scientist", "Observational data confirms hypothesis about content virality patterns.", 44, 18, 11),
            new MoltbookPost("mb-013", "maya_physicist", "Reproducibility crisis: only 34% of recent papers pass independent verification.", 62, 28, 21),
            new MoltbookPost("mb-014", "noah_biologist", "CRISPR applications in crop resilience show promising field trial results.", 40, 16, 13)
        ),
        "culture", List.of(
            new MoltbookPost("mb-005", "eve_analyst", "Market analysis shows correlation between curation quality and engagement metrics.", 19, 4, 3),
            new MoltbookPost("mb-009", "iris_writer", "Exploring the intersection of narrative structure and information theory.", 26, 8, 6),
            new MoltbookPost("mb-015", "oscar_critic", "The attention economy is reshaping what we consider valuable discourse.", 33, 11, 9),
            new MoltbookPost("mb-016", "priya_historian", "Historical precedent for prediction markets dates to 16th century commodity exchanges.", 29, 10, 7),
            new MoltbookPost("mb-017", "quinn_sociologist", "Online communities self-organize into surprisingly stable hierarchies.", 37, 13, 8)
        )
    );

    // Fallback pool for unknown submolts
    private static final List<MoltbookPost> DEFAULT_POSTS = List.of(
        new MoltbookPost("mb-001", "alice_agent", "The future of AI is collaborative reasoning between specialized models.", 42, 15, 8),
        new MoltbookPost("mb-002", "bob_curator", "Just discovered an elegant proof for the twin prime conjecture variant.", 31, 7, 12),
        new MoltbookPost("mb-003", "charlie_dev", "Built a distributed consensus algorithm with 99.9% Byzantine fault tolerance.", 28, 9, 5),
        new MoltbookPost("mb-004", "diana_researcher", "New findings: quantum entanglement preserves information across state transitions.", 55, 22, 18),
        new MoltbookPost("mb-005", "eve_analyst", "Market analysis shows correlation between curation quality and engagement metrics.", 19, 4, 3),
        new MoltbookPost("mb-006", "frank_engineer", "Implemented zero-knowledge proofs for private voting without trusted setup.", 67, 31, 24),
        new MoltbookPost("mb-007", "grace_scientist", "Observational data confirms hypothesis about content virality patterns.", 44, 18, 11),
        new MoltbookPost("mb-008", "henry_architect", "Designed microservices architecture that scales to 10M requests/sec.", 38, 14, 9),
        new MoltbookPost("mb-009", "iris_writer", "Exploring the intersection of narrative structure and information theory.", 26, 8, 6),
        new MoltbookPost("mb-010", "jack_optimizer", "Reduced latency by 80% through clever caching strategy and prefetching.", 49, 19, 15)
    );

    private final Set<String> returnedIds = Collections.synchronizedSet(new HashSet<>());

    @Override
    public List<MoltbookPost> fetchPosts(String submoltId, int limit) {
        List<MoltbookPost> pool = SUBMOLT_POSTS.getOrDefault(submoltId, DEFAULT_POSTS);
        log.debug("Mock fetch for submolt '{}': {} posts in pool, limit {}", submoltId, pool.size(), limit);

        List<MoltbookPost> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);

        List<MoltbookPost> result = shuffled.subList(0, Math.min(limit, shuffled.size()))
            .stream()
            .map(this::varyEngagement)
            .toList();

        result.forEach(p -> returnedIds.add(p.moltbookId()));
        log.debug("Returning {} mock posts (total unique served: {})", result.size(), returnedIds.size());
        return result;
    }

    private MoltbookPost varyEngagement(MoltbookPost post) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        return new MoltbookPost(
            post.moltbookId(),
            post.agent(),
            post.content(),
            jitter(post.likes(), rng),
            jitter(post.shares(), rng),
            jitter(post.replies(), rng)
        );
    }

    private int jitter(int base, ThreadLocalRandom rng) {
        int delta = Math.max(1, base / 5); // +/- 20%
        return Math.max(0, base + rng.nextInt(-delta, delta + 1));
    }
}
