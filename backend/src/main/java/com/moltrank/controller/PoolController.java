package com.moltrank.controller;

import com.moltrank.model.GlobalPool;
import com.moltrank.repository.GlobalPoolRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for GlobalPool health data.
 */
@RestController
@RequestMapping("/api/pool")
public class PoolController {

    private final GlobalPoolRepository globalPoolRepository;

    public PoolController(GlobalPoolRepository globalPoolRepository) {
        this.globalPoolRepository = globalPoolRepository;
    }

    /**
     * Get GlobalPool health data.
     *
     * @return GlobalPool with balance, alpha, current round, and settlement hash
     */
    @GetMapping
    public ResponseEntity<GlobalPool> getPoolHealth() {
        // GlobalPool has singleton ID of 1
        GlobalPool pool = globalPoolRepository.findById(1)
                .orElse(null);

        if (pool == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(pool);
    }
}
