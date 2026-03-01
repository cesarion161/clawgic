package com.clawgic.clawgic.controller;

import com.clawgic.clawgic.dto.ClawgicMatchResponses;
import com.clawgic.clawgic.dto.ClawgicTournamentResponses;
import com.clawgic.clawgic.service.ClawgicMatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clawgic")
public class ClawgicMatchController {

    private final ClawgicMatchService clawgicMatchService;

    public ClawgicMatchController(ClawgicMatchService clawgicMatchService) {
        this.clawgicMatchService = clawgicMatchService;
    }

    @GetMapping("/matches/{matchId}")
    public ResponseEntity<ClawgicMatchResponses.MatchDetail> getMatch(@PathVariable UUID matchId) {
        return ResponseEntity.ok(clawgicMatchService.getMatchDetail(matchId));
    }

    @GetMapping("/tournaments/{tournamentId}/matches")
    public ResponseEntity<List<ClawgicMatchResponses.MatchSummary>> listTournamentMatches(
            @PathVariable UUID tournamentId
    ) {
        return ResponseEntity.ok(clawgicMatchService.listTournamentMatches(tournamentId));
    }

    @GetMapping("/tournaments/{tournamentId}/live")
    public ResponseEntity<ClawgicTournamentResponses.TournamentLiveStatus> getTournamentLiveStatus(
            @PathVariable UUID tournamentId
    ) {
        return ResponseEntity.ok(clawgicMatchService.getTournamentLiveStatus(tournamentId));
    }
}
