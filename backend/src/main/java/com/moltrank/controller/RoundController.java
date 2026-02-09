package com.moltrank.controller;

import com.moltrank.model.Round;
import com.moltrank.repository.RoundRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for round management.
 */
@RestController
@RequestMapping("/api/rounds")
public class RoundController {

    private final RoundRepository roundRepository;

    public RoundController(RoundRepository roundRepository) {
        this.roundRepository = roundRepository;
    }

    /**
     * List all rounds with status.
     *
     * @param marketId Optional market ID filter (defaults to 1)
     * @return List of rounds sorted by created date (descending)
     */
    @GetMapping
    public ResponseEntity<List<Round>> listRounds(
            @RequestParam(defaultValue = "1") Integer marketId) {

        List<Round> rounds = roundRepository.findByMarketId(
                marketId,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(rounds);
    }

    /**
     * Get round detail with settlement hash.
     *
     * @param id The round ID
     * @return Round details including status, pairs, deadlines, and settlement hash
     */
    @GetMapping("/{id}")
    public ResponseEntity<Round> getRoundDetail(@PathVariable Integer id) {
        Round round = roundRepository.findById(id)
                .orElse(null);

        if (round == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(round);
    }
}
