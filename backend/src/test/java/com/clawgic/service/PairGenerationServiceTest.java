package com.clawgic.service;

import com.clawgic.model.*;
import com.clawgic.repository.GoldenSetItemRepository;
import com.clawgic.repository.PairRepository;
import com.clawgic.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PairGenerationServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PairRepository pairRepository;

    @Mock
    private GoldenSetItemRepository goldenSetItemRepository;

    @InjectMocks
    private PairGenerationService pairGenerationService;

    private Market market;
    private Round round;
    private List<Post> candidatePosts;

    @BeforeEach
    void setUp() {
        // Set K value
        ReflectionTestUtils.setField(pairGenerationService, "pairsPerSubscriber", 5);

        market = new Market();
        market.setId(1);
        market.setName("tech");
        market.setSubmoltId("tech");
        market.setSubscribers(10);

        round = new Round();
        round.setId(1);
        round.setMarket(market);
        round.setStatus(RoundStatus.OPEN);

        // Create 20 candidate posts
        candidatePosts = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Post post = new Post();
            post.setId(i);
            post.setMoltbookId("mb-" + i);
            post.setAgent("Agent" + i);
            post.setContent("Content for post " + i);
            post.setElo(1500);
            post.setMarket(market);
            candidatePosts.add(post);
        }
    }

    @Test
    void generatePairs_demandGatedFormula_subscriberLimitApplied() {
        // 20 posts = 10 max from posts, 10*5=50 max from subscribers -> min(10,50)=10
        when(postRepository.findByMarketId(1)).thenReturn(candidatePosts);
        when(goldenSetItemRepository.findAll()).thenReturn(List.of());
        when(pairRepository.save(any(Pair.class))).thenAnswer(inv -> {
            Pair p = inv.getArgument(0);
            p.setId((int) (Math.random() * 10000));
            return p;
        });

        List<Pair> pairs = pairGenerationService.generatePairs(round);

        // maxPairs = min(20/2, 10*5) = 10
        // golden attempts 1 but no golden items available -> 0
        // audit = 1 (swapped from existing regular pairs)
        // regular = 10 - 1 - 1 = 8
        // total = 8 + 0 + 1 = 9
        assertEquals(9, pairs.size(), "Should generate maxPairs minus unavailable golden pairs");
        long regularCount = pairs.stream().filter(p -> !p.getIsGolden() && !p.getIsAudit()).count();
        long auditCount = pairs.stream().filter(Pair::getIsAudit).count();
        assertEquals(8, regularCount, "Should have 8 regular pairs");
        assertEquals(1, auditCount, "Should have 1 audit pair");
    }

    @Test
    void generatePairs_postsLimitApplied() {
        // 6 posts = 3 max from posts, 10*5=50 from subscribers -> min(3,50)=3
        List<Post> fewPosts = candidatePosts.subList(0, 6);
        when(postRepository.findByMarketId(1)).thenReturn(fewPosts);
        when(goldenSetItemRepository.findAll()).thenReturn(List.of());
        when(pairRepository.save(any(Pair.class))).thenAnswer(inv -> {
            Pair p = inv.getArgument(0);
            p.setId((int) (Math.random() * 10000));
            return p;
        });

        List<Pair> pairs = pairGenerationService.generatePairs(round);

        // maxPairs = min(6/2, 50) = 3
        // golden=ceil(0.3)=1 (no items), audit=ceil(0.15)=1, regular=3-1-1=1
        // actual: regular=1, golden=0, audit=1 -> 2 total
        assertTrue(pairs.size() <= 3, "Should be capped by posts/2, got: " + pairs.size());
        assertTrue(pairs.size() >= 1, "Should generate at least 1 pair");
    }

    @Test
    void generatePairs_zeroSubscribers_returnsEmpty() {
        market.setSubscribers(0);
        when(postRepository.findByMarketId(1)).thenReturn(candidatePosts);

        List<Pair> pairs = pairGenerationService.generatePairs(round);

        assertTrue(pairs.isEmpty(), "Zero subscribers should produce zero pairs");
    }

    @Test
    void generatePairs_onlyOnePost_returnsEmpty() {
        when(postRepository.findByMarketId(1)).thenReturn(candidatePosts.subList(0, 1));

        List<Pair> pairs = pairGenerationService.generatePairs(round);

        assertTrue(pairs.isEmpty(), "Single post cannot form a pair");
    }

    @Test
    void generatePairs_includesGoldenPairs() {
        GoldenSetItem goldenItem = new GoldenSetItem();
        goldenItem.setId(1);
        goldenItem.setPostA(candidatePosts.get(0));
        goldenItem.setPostB(candidatePosts.get(1));
        goldenItem.setCorrectAnswer(PairWinner.A);
        goldenItem.setConfidence(new BigDecimal("0.95"));
        goldenItem.setSource("test");

        when(postRepository.findByMarketId(1)).thenReturn(candidatePosts);
        when(goldenSetItemRepository.findAll()).thenReturn(List.of(goldenItem));
        when(pairRepository.save(any(Pair.class))).thenAnswer(inv -> {
            Pair p = inv.getArgument(0);
            p.setId((int) (Math.random() * 10000));
            return p;
        });

        List<Pair> pairs = pairGenerationService.generatePairs(round);

        long goldenCount = pairs.stream().filter(Pair::getIsGolden).count();
        assertTrue(goldenCount >= 1, "Should include at least one golden pair");
    }

    @Test
    void generatePairs_includesAuditPairs() {
        when(postRepository.findByMarketId(1)).thenReturn(candidatePosts);
        when(goldenSetItemRepository.findAll()).thenReturn(List.of());
        when(pairRepository.save(any(Pair.class))).thenAnswer(inv -> {
            Pair p = inv.getArgument(0);
            p.setId((int) (Math.random() * 10000));
            return p;
        });

        List<Pair> pairs = pairGenerationService.generatePairs(round);

        long auditCount = pairs.stream().filter(Pair::getIsAudit).count();
        assertTrue(auditCount >= 1, "Should include at least one audit pair");
    }

    @Test
    void generatePairs_auditPairsAreSwapped() {
        when(postRepository.findByMarketId(1)).thenReturn(candidatePosts);
        when(goldenSetItemRepository.findAll()).thenReturn(List.of());
        when(pairRepository.save(any(Pair.class))).thenAnswer(inv -> {
            Pair p = inv.getArgument(0);
            p.setId((int) (Math.random() * 10000));
            return p;
        });

        List<Pair> pairs = pairGenerationService.generatePairs(round);

        List<Pair> auditPairs = pairs.stream().filter(Pair::getIsAudit).toList();
        List<Pair> regularPairs = pairs.stream()
                .filter(p -> !p.getIsAudit() && !p.getIsGolden())
                .toList();

        for (Pair audit : auditPairs) {
            // Check if there exists a regular pair with swapped posts
            boolean foundOriginal = regularPairs.stream().anyMatch(r ->
                    r.getPostA().getId().equals(audit.getPostB().getId()) &&
                    r.getPostB().getId().equals(audit.getPostA().getId()));
            assertTrue(foundOriginal, "Audit pair should be a swapped version of an existing pair");
        }
    }

    @Test
    void generatePairs_noDuplicateRegularPairs() {
        when(postRepository.findByMarketId(1)).thenReturn(candidatePosts);
        when(goldenSetItemRepository.findAll()).thenReturn(List.of());
        when(pairRepository.save(any(Pair.class))).thenAnswer(inv -> {
            Pair p = inv.getArgument(0);
            p.setId((int) (Math.random() * 10000));
            return p;
        });

        List<Pair> pairs = pairGenerationService.generatePairs(round);

        List<Pair> regularPairs = pairs.stream()
                .filter(p -> !p.getIsAudit() && !p.getIsGolden())
                .toList();

        // Check no duplicates (order-independent)
        java.util.Set<String> signatures = new java.util.HashSet<>();
        for (Pair p : regularPairs) {
            int min = Math.min(p.getPostA().getId(), p.getPostB().getId());
            int max = Math.max(p.getPostA().getId(), p.getPostB().getId());
            String sig = min + "-" + max;
            assertTrue(signatures.add(sig), "Duplicate pair found: " + sig);
        }
    }

    @Test
    void generatePairs_noPairWithSamePost() {
        when(postRepository.findByMarketId(1)).thenReturn(candidatePosts);
        when(goldenSetItemRepository.findAll()).thenReturn(List.of());
        when(pairRepository.save(any(Pair.class))).thenAnswer(inv -> {
            Pair p = inv.getArgument(0);
            p.setId((int) (Math.random() * 10000));
            return p;
        });

        List<Pair> pairs = pairGenerationService.generatePairs(round);

        for (Pair p : pairs) {
            assertNotEquals(p.getPostA().getId(), p.getPostB().getId(),
                    "A pair should not have the same post on both sides");
        }
    }

    @Test
    void generatePairs_goldenPairRatio_approximately10percent() {
        // With enough posts and subscribers, golden should be ~10%
        market.setSubscribers(100); // Lots of demand
        when(postRepository.findByMarketId(1)).thenReturn(candidatePosts);

        // Create multiple golden set items
        List<GoldenSetItem> goldenItems = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            GoldenSetItem item = new GoldenSetItem();
            item.setId(i + 1);
            item.setPostA(candidatePosts.get(i * 2));
            item.setPostB(candidatePosts.get(i * 2 + 1));
            item.setCorrectAnswer(PairWinner.A);
            item.setConfidence(new BigDecimal("0.90"));
            item.setSource("test");
            goldenItems.add(item);
        }

        when(goldenSetItemRepository.findAll()).thenReturn(goldenItems);
        when(pairRepository.save(any(Pair.class))).thenAnswer(inv -> {
            Pair p = inv.getArgument(0);
            p.setId((int) (Math.random() * 10000));
            return p;
        });

        List<Pair> pairs = pairGenerationService.generatePairs(round);

        long goldenCount = pairs.stream().filter(Pair::getIsGolden).count();
        double goldenRatio = (double) goldenCount / pairs.size();
        assertTrue(goldenRatio >= 0.05 && goldenRatio <= 0.20,
                "Golden pair ratio should be approximately 10%, got: " + String.format("%.2f", goldenRatio));
    }
}
