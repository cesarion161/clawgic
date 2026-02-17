package com.moltrank.controller;

import com.moltrank.controller.dto.PairResponse;
import com.moltrank.model.Commitment;
import com.moltrank.model.Pair;
import com.moltrank.repository.CommitmentRepository;
import com.moltrank.repository.PairRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

/**
 * REST API for pairwise curation.
 */
@RestController
@RequestMapping("/api/pairs")
public class PairsController {

    private final PairRepository pairRepository;
    private final CommitmentRepository commitmentRepository;

    public PairsController(PairRepository pairRepository,
                           CommitmentRepository commitmentRepository) {
        this.pairRepository = pairRepository;
        this.commitmentRepository = commitmentRepository;
    }

    /**
     * Get next pair for curation.
     *
     * @param wallet The curator's wallet address
     * @param marketId Optional market ID (defaults to 1)
     * @return Next pair to curate, or 404 if no pairs available
     */
    @GetMapping("/next")
    public ResponseEntity<PairResponse> getNextPair(
            @RequestParam String wallet,
            @RequestParam(defaultValue = "1") Integer marketId) {

        // Find first uncommitted pair for this curator
        Pair nextPair = pairRepository.findNextPairForCurator(wallet, marketId)
                .orElse(null);

        if (nextPair == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(PairResponse.from(nextPair));
    }

    /**
     * Submit commitment for a pair.
     *
     * @param id The pair ID
     * @param commitment The commitment data
     * @return Created commitment
     */
    @PostMapping("/{id}/commit")
    public ResponseEntity<Void> commitPair(
            @PathVariable Integer id,
            @RequestBody Commitment commitment) {

        // Verify pair exists
        Pair pair = pairRepository.findById(id)
                .orElse(null);

        if (pair == null) {
            return ResponseEntity.notFound().build();
        }

        // Set pair reference and timestamp
        commitment.setPair(pair);
        OffsetDateTime committedAt = OffsetDateTime.now();
        commitment.setCommittedAt(committedAt);
        commitment.setRevealed(false);

        // Save commitment
        commitmentRepository.save(commitment);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
