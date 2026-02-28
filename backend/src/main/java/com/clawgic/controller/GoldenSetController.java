package com.clawgic.controller;

import com.clawgic.controller.dto.GoldenSetItemResponse;
import com.clawgic.model.GoldenSetItem;
import com.clawgic.service.GoldenSetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for managing golden set items.
 */
@RestController
@RequestMapping("/api/golden-set")
public class GoldenSetController {

    private final GoldenSetService goldenSetService;

    public GoldenSetController(GoldenSetService goldenSetService) {
        this.goldenSetService = goldenSetService;
    }

    /**
     * Get all golden set items.
     *
     * @return List of all golden set items
     */
    @GetMapping
    public ResponseEntity<List<GoldenSetItemResponse>> getAllGoldenSetItems() {
        List<GoldenSetItem> items = goldenSetService.getAllGoldenSetItems();
        List<GoldenSetItemResponse> response = items.stream()
                .map(GoldenSetItemResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Add a new golden set item.
     *
     * @param item The golden set item to add
     * @return The created golden set item
     */
    @PostMapping
    public ResponseEntity<Void> addGoldenSetItem(@RequestBody GoldenSetItem item) {
        goldenSetService.addGoldenSetItem(item);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Delete a golden set item.
     *
     * @param id The ID of the golden set item to delete
     * @return No content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoldenSetItem(@PathVariable Integer id) {
        goldenSetService.deleteGoldenSetItem(id);
        return ResponseEntity.noContent().build();
    }
}
