package com.clawgic.controller;

import com.clawgic.controller.dto.AgentProfileResponse;
import com.clawgic.controller.dto.PostResponse;
import com.clawgic.model.Post;
import com.clawgic.repository.PostRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<AgentProfileResponse> getAgentProfile(@PathVariable String id) {
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
        List<PostResponse> postResponses = posts.stream()
                .map(PostResponse::from)
                .toList();

        AgentProfileResponse response = new AgentProfileResponse(
                id,
                totalPosts,
                totalMatchups,
                totalWins,
                (int) Math.round(avgElo),
                maxElo,
                totalMatchups > 0 ? (double) totalWins / totalMatchups : 0.0,
                postResponses
        );

        return ResponseEntity.ok(response);
    }
}
