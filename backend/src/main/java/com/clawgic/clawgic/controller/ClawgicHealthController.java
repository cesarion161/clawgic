package com.clawgic.clawgic.controller;

import com.clawgic.clawgic.dto.ClawgicHealthResponse;
import com.clawgic.clawgic.service.ClawgicHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal Clawgic endpoint to verify package wiring before domain logic lands.
 */
@RestController
@RequestMapping("/api/clawgic")
public class ClawgicHealthController {

    private final ClawgicHealthService clawgicHealthService;

    public ClawgicHealthController(ClawgicHealthService clawgicHealthService) {
        this.clawgicHealthService = clawgicHealthService;
    }

    @GetMapping("/health")
    public ResponseEntity<ClawgicHealthResponse> health() {
        return ResponseEntity.ok(clawgicHealthService.health());
    }
}
