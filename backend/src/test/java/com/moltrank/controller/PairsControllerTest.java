package com.moltrank.controller;

import com.moltrank.model.*;
import com.moltrank.repository.CommitmentRepository;
import com.moltrank.repository.PairRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for PairsController API endpoints.
 * Tests the frontend → backend API flow for pair curation.
 */
@WebMvcTest(PairsController.class)
class PairsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PairRepository pairRepository;

    @MockitoBean
    private CommitmentRepository commitmentRepository;

    private static final String WALLET = "4Nd1mYQzvgV8Vr3Z3nYb7pD6T8K9jF2eqWxY1S3Qh5Ro";

    private Pair buildPair() {
        Market market = new Market();
        market.setId(1);
        market.setName("tech");
        market.setSubmoltId("tech");

        Round round = new Round();
        round.setId(1);
        round.setMarket(market);
        round.setStatus(RoundStatus.OPEN);

        Post postA = new Post();
        postA.setId(1);
        postA.setMoltbookId("post-a-001");
        postA.setAgent("agent-alpha");
        postA.setContent("First post content");
        postA.setElo(1500);

        Post postB = new Post();
        postB.setId(2);
        postB.setMoltbookId("post-b-002");
        postB.setAgent("agent-beta");
        postB.setContent("Second post content");
        postB.setElo(1520);

        Pair pair = new Pair();
        pair.setId(1);
        pair.setRound(round);
        pair.setPostA(postA);
        pair.setPostB(postB);
        pair.setTotalStake(0L);
        pair.setReward(0L);
        pair.setIsGolden(false);
        pair.setIsAudit(false);
        return pair;
    }

    @Test
    void getNextPair_returnsPairForCurator() throws Exception {
        Pair pair = buildPair();
        when(pairRepository.findNextPairForCurator(WALLET, 1))
                .thenReturn(Optional.of(pair));

        mockMvc.perform(get("/api/pairs/next")
                        .param("wallet", WALLET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.isGolden").value(false))
                .andExpect(jsonPath("$.isAudit").value(false));
    }

    @Test
    void getNextPair_returns404WhenNoPairsAvailable() throws Exception {
        when(pairRepository.findNextPairForCurator(WALLET, 1))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/pairs/next")
                        .param("wallet", WALLET))
                .andExpect(status().isNotFound());
    }

    @Test
    void commitPair_createsCommitment() throws Exception {
        Pair pair = buildPair();
        when(pairRepository.findById(1)).thenReturn(Optional.of(pair));

        Commitment saved = new Commitment();
        saved.setId(1);
        saved.setPair(pair);
        saved.setCuratorWallet(WALLET);
        saved.setHash("0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890ab");
        saved.setStake(1000000000L);
        saved.setEncryptedReveal("encrypted-payload");
        saved.setRevealed(false);

        when(commitmentRepository.save(any(Commitment.class))).thenReturn(saved);

        String requestBody = """
                {
                    "curatorWallet": "%s",
                    "hash": "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890ab",
                    "stake": 1000000000,
                    "encryptedReveal": "encrypted-payload"
                }
                """.formatted(WALLET);

        mockMvc.perform(post("/api/pairs/{id}/commit", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.curatorWallet").value(WALLET))
                .andExpect(jsonPath("$.stake").value(1000000000))
                .andExpect(jsonPath("$.revealed").value(false));
    }

    @Test
    void commitPair_returns404ForInvalidPair() throws Exception {
        when(pairRepository.findById(999)).thenReturn(Optional.empty());

        String requestBody = """
                {
                    "curatorWallet": "%s",
                    "hash": "0xabcdef",
                    "stake": 1000000000,
                    "encryptedReveal": "encrypted-payload"
                }
                """.formatted(WALLET);

        mockMvc.perform(post("/api/pairs/{id}/commit", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }
}
