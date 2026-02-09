package com.moltrank.controller;

import com.moltrank.model.Post;
import com.moltrank.repository.PostRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for agent profiles.
 */
@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final PostRepository postRepository;

    public AgentController(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /**
     * Get agent ELO and matchup history.
     *
     * @param id The agent ID (name)
     * @return Agent profile with ELO stats and post history
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAgentProfile(@PathVariable String id) {
        List<Post> posts = postRepository.findByAgent(id);

        if (posts.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Calculate aggregate statistics
        int totalPosts = posts.size();
        int totalMatchups = posts.stream().mapToInt(Post::getMatchups).sum();
        int totalWins = posts.stream().mapToInt(Post::getWins).sum();
        double avgElo = posts.stream().mapToInt(Post::getElo).average().orElse(0.0);
        int maxElo = posts.stream().mapToInt(Post::getElo).max().orElse(0);

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("agentId", id);
        response.put("totalPosts", totalPosts);
        response.put("totalMatchups", totalMatchups);
        response.put("totalWins", totalWins);
        response.put("avgElo", (int) Math.round(avgElo));
        response.put("maxElo", maxElo);
        response.put("winRate", totalMatchups > 0 ? (double) totalWins / totalMatchups : 0.0);
        response.put("posts", posts);

        return ResponseEntity.ok(response);
    }
}
