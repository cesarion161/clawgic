package com.clawgic.clawgic.controller;

import com.clawgic.clawgic.dto.ClawgicMatchResponses;
import com.clawgic.clawgic.dto.ClawgicTournamentResponses;
import com.clawgic.clawgic.model.ClawgicMatchJudgementStatus;
import com.clawgic.clawgic.model.ClawgicMatchStatus;
import com.clawgic.clawgic.model.ClawgicTournamentStatus;
import com.clawgic.clawgic.model.DebatePhase;
import com.clawgic.clawgic.service.ClawgicMatchService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClawgicMatchController.class)
class ClawgicMatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClawgicMatchService clawgicMatchService;

    private static final UUID MATCH_ID = UUID.fromString("00000000-0000-0000-0000-000000000801");
    private static final UUID TOURNAMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID AGENT_1_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID AGENT_2_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-01T14:00:00Z");

    @Test
    void getMatchReturnsDetailPayload() throws Exception {
        when(clawgicMatchService.getMatchDetail(MATCH_ID)).thenReturn(sampleMatchDetail());

        mockMvc.perform(get("/api/clawgic/matches/{matchId}", MATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(MATCH_ID.toString()))
                .andExpect(jsonPath("$.tournamentId").value(TOURNAMENT_ID.toString()))
                .andExpect(jsonPath("$.agent1Id").value(AGENT_1_ID.toString()))
                .andExpect(jsonPath("$.agent2Id").value(AGENT_2_ID.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.phase").value("ARGUMENTATION"))
                .andExpect(jsonPath("$.bracketRound").value(1))
                .andExpect(jsonPath("$.bracketPosition").value(1))
                .andExpect(jsonPath("$.transcriptJson").exists())
                .andExpect(jsonPath("$.transcriptJson.array").value(true))
                .andExpect(jsonPath("$.judgements").isArray());
    }

    @Test
    void getMatchNotFoundReturns404() throws Exception {
        UUID unknownId = UUID.fromString("00000000-0000-0000-0000-999999999999");
        when(clawgicMatchService.getMatchDetail(unknownId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found: " + unknownId));

        mockMvc.perform(get("/api/clawgic/matches/{matchId}", unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMatchCompletedReturnsWinnerAndElo() throws Exception {
        var detail = sampleCompletedMatchDetail();
        when(clawgicMatchService.getMatchDetail(MATCH_ID)).thenReturn(detail);

        mockMvc.perform(get("/api/clawgic/matches/{matchId}", MATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.winnerAgentId").value(AGENT_1_ID.toString()))
                .andExpect(jsonPath("$.agent1EloBefore").value(1000))
                .andExpect(jsonPath("$.agent1EloAfter").value(1016))
                .andExpect(jsonPath("$.agent2EloBefore").value(1000))
                .andExpect(jsonPath("$.agent2EloAfter").value(984))
                .andExpect(jsonPath("$.judgements").isArray())
                .andExpect(jsonPath("$.judgements[0].status").value("ACCEPTED"));
    }

    @Test
    void getMatchForfeitedReturnsForfeitReason() throws Exception {
        var detail = sampleForfeitedMatchDetail();
        when(clawgicMatchService.getMatchDetail(MATCH_ID)).thenReturn(detail);

        mockMvc.perform(get("/api/clawgic/matches/{matchId}", MATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FORFEITED"))
                .andExpect(jsonPath("$.forfeitReason").value("PROVIDER_TIMEOUT: agent2 timed out during ARGUMENTATION"))
                .andExpect(jsonPath("$.winnerAgentId").value(AGENT_1_ID.toString()));
    }

    @Test
    void listTournamentMatchesReturnsMatchSummaries() throws Exception {
        when(clawgicMatchService.listTournamentMatches(TOURNAMENT_ID)).thenReturn(sampleMatchSummaries());

        mockMvc.perform(get("/api/clawgic/tournaments/{tournamentId}/matches", TOURNAMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].matchId").exists())
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].bracketRound").value(1))
                .andExpect(jsonPath("$[1].bracketRound").value(1))
                .andExpect(jsonPath("$[2].bracketRound").value(2));
    }

    @Test
    void listTournamentMatchesNotFoundReturns404() throws Exception {
        UUID unknownId = UUID.fromString("00000000-0000-0000-0000-999999999999");
        when(clawgicMatchService.listTournamentMatches(unknownId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament not found: " + unknownId));

        mockMvc.perform(get("/api/clawgic/tournaments/{tournamentId}/matches", unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    void listTournamentMatchesEmptyBracketReturnsEmptyArray() throws Exception {
        when(clawgicMatchService.listTournamentMatches(TOURNAMENT_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/clawgic/tournaments/{tournamentId}/matches", TOURNAMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private ClawgicMatchResponses.MatchDetail sampleMatchDetail() {
        return new ClawgicMatchResponses.MatchDetail(
                MATCH_ID, TOURNAMENT_ID, AGENT_1_ID, AGENT_2_ID,
                1, 1, null, null,
                ClawgicMatchStatus.IN_PROGRESS, DebatePhase.ARGUMENTATION,
                JsonNodeFactory.instance.arrayNode(), null,
                null, null, null, null, null,
                null, 0, List.of(),
                NOW.plusMinutes(30), null,
                NOW, null, null, null, null, NOW, NOW
        );
    }

    private ClawgicMatchResponses.MatchDetail sampleCompletedMatchDetail() {
        UUID judgementId = UUID.fromString("00000000-0000-0000-0000-000000000901");
        var judgement = new ClawgicMatchResponses.MatchJudgementSummary(
                judgementId, MATCH_ID, "openai-gpt4o", "gpt-4o",
                ClawgicMatchJudgementStatus.ACCEPTED, 1,
                JsonNodeFactory.instance.objectNode(), AGENT_1_ID,
                8, 7, 9, 6, 5, 4,
                "Agent 1 demonstrated stronger logic and rebuttals.",
                NOW.plusMinutes(35), NOW.plusMinutes(34), NOW.plusMinutes(35)
        );

        return new ClawgicMatchResponses.MatchDetail(
                MATCH_ID, TOURNAMENT_ID, AGENT_1_ID, AGENT_2_ID,
                1, 1, null, null,
                ClawgicMatchStatus.COMPLETED, DebatePhase.CONCLUSION,
                JsonNodeFactory.instance.arrayNode(), JsonNodeFactory.instance.objectNode(),
                AGENT_1_ID, 1000, 1016, 1000, 984,
                null, 0, List.of(judgement),
                NOW.plusMinutes(30), NOW.plusMinutes(60),
                NOW, NOW.plusMinutes(30), NOW.plusMinutes(35), null, NOW.plusMinutes(35),
                NOW, NOW.plusMinutes(35)
        );
    }

    private ClawgicMatchResponses.MatchDetail sampleForfeitedMatchDetail() {
        return new ClawgicMatchResponses.MatchDetail(
                MATCH_ID, TOURNAMENT_ID, AGENT_1_ID, AGENT_2_ID,
                1, 1, null, null,
                ClawgicMatchStatus.FORFEITED, DebatePhase.ARGUMENTATION,
                JsonNodeFactory.instance.arrayNode(), null,
                AGENT_1_ID, null, null, null, null,
                "PROVIDER_TIMEOUT: agent2 timed out during ARGUMENTATION", 0, List.of(),
                NOW.plusMinutes(30), null,
                NOW, null, null, NOW.plusMinutes(5), null, NOW, NOW.plusMinutes(5)
        );
    }

    @Test
    void getTournamentLiveStatusReturnsInProgressState() throws Exception {
        UUID match1Id = UUID.fromString("00000000-0000-0000-0000-000000000801");
        UUID match2Id = UUID.fromString("00000000-0000-0000-0000-000000000802");
        UUID match3Id = UUID.fromString("00000000-0000-0000-0000-000000000803");

        var liveStatus = new ClawgicTournamentResponses.TournamentLiveStatus(
                TOURNAMENT_ID,
                "AI Debate Championship",
                ClawgicTournamentStatus.IN_PROGRESS,
                NOW,
                NOW.minusHours(1),
                NOW.plusMinutes(5),
                match1Id,
                null,
                0, 0,
                List.of(
                        new ClawgicTournamentResponses.BracketMatchStatus(
                                match1Id, ClawgicMatchStatus.IN_PROGRESS, DebatePhase.ARGUMENTATION,
                                AGENT_1_ID, AGENT_2_ID, null, 1, 1
                        ),
                        new ClawgicTournamentResponses.BracketMatchStatus(
                                match2Id, ClawgicMatchStatus.SCHEDULED, null,
                                null, null, null, 1, 2
                        ),
                        new ClawgicTournamentResponses.BracketMatchStatus(
                                match3Id, ClawgicMatchStatus.SCHEDULED, null,
                                null, null, null, 2, 1
                        )
                )
        );
        when(clawgicMatchService.getTournamentLiveStatus(TOURNAMENT_ID)).thenReturn(liveStatus);

        mockMvc.perform(get("/api/clawgic/tournaments/{tournamentId}/live", TOURNAMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tournamentId").value(TOURNAMENT_ID.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.topic").value("AI Debate Championship"))
                .andExpect(jsonPath("$.serverTime").exists())
                .andExpect(jsonPath("$.activeMatchId").value(match1Id.toString()))
                .andExpect(jsonPath("$.tournamentWinnerAgentId").doesNotExist())
                .andExpect(jsonPath("$.bracket").isArray())
                .andExpect(jsonPath("$.bracket.length()").value(3))
                .andExpect(jsonPath("$.bracket[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.bracket[0].phase").value("ARGUMENTATION"))
                .andExpect(jsonPath("$.bracket[1].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.bracket[2].bracketRound").value(2));
    }

    @Test
    void getTournamentLiveStatusReturnsCompletedWithWinner() throws Exception {
        var liveStatus = new ClawgicTournamentResponses.TournamentLiveStatus(
                TOURNAMENT_ID,
                "Final Showdown",
                ClawgicTournamentStatus.COMPLETED,
                NOW,
                NOW.minusHours(1),
                NOW.plusHours(2),
                null,
                AGENT_1_ID,
                3, 0,
                List.of()
        );
        when(clawgicMatchService.getTournamentLiveStatus(TOURNAMENT_ID)).thenReturn(liveStatus);

        mockMvc.perform(get("/api/clawgic/tournaments/{tournamentId}/live", TOURNAMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.tournamentWinnerAgentId").value(AGENT_1_ID.toString()))
                .andExpect(jsonPath("$.activeMatchId").doesNotExist())
                .andExpect(jsonPath("$.matchesCompleted").value(3));
    }

    @Test
    void getTournamentLiveStatusNotFoundReturns404() throws Exception {
        UUID unknownId = UUID.fromString("00000000-0000-0000-0000-999999999999");
        when(clawgicMatchService.getTournamentLiveStatus(unknownId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament not found: " + unknownId));

        mockMvc.perform(get("/api/clawgic/tournaments/{tournamentId}/live", unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTournamentLiveStatusScheduledShowsNoBracket() throws Exception {
        var liveStatus = new ClawgicTournamentResponses.TournamentLiveStatus(
                TOURNAMENT_ID,
                "Upcoming Tournament",
                ClawgicTournamentStatus.SCHEDULED,
                NOW.plusHours(2),
                NOW.plusHours(1),
                NOW,
                null,
                null,
                0, 0,
                List.of()
        );
        when(clawgicMatchService.getTournamentLiveStatus(TOURNAMENT_ID)).thenReturn(liveStatus);

        mockMvc.perform(get("/api/clawgic/tournaments/{tournamentId}/live", TOURNAMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.entryCloseTime").exists())
                .andExpect(jsonPath("$.activeMatchId").doesNotExist())
                .andExpect(jsonPath("$.bracket").isArray())
                .andExpect(jsonPath("$.bracket.length()").value(0));
    }

    private List<ClawgicMatchResponses.MatchSummary> sampleMatchSummaries() {
        UUID match1Id = UUID.fromString("00000000-0000-0000-0000-000000000801");
        UUID match2Id = UUID.fromString("00000000-0000-0000-0000-000000000802");
        UUID match3Id = UUID.fromString("00000000-0000-0000-0000-000000000803");
        UUID agent3Id = UUID.fromString("00000000-0000-0000-0000-000000000103");
        UUID agent4Id = UUID.fromString("00000000-0000-0000-0000-000000000104");

        return List.of(
                new ClawgicMatchResponses.MatchSummary(
                        match1Id, TOURNAMENT_ID, AGENT_1_ID, AGENT_2_ID,
                        1, 1, match3Id, 1,
                        ClawgicMatchStatus.IN_PROGRESS, DebatePhase.ARGUMENTATION,
                        null, NOW, NOW
                ),
                new ClawgicMatchResponses.MatchSummary(
                        match2Id, TOURNAMENT_ID, agent3Id, agent4Id,
                        1, 2, match3Id, 2,
                        ClawgicMatchStatus.SCHEDULED, null,
                        null, NOW, NOW
                ),
                new ClawgicMatchResponses.MatchSummary(
                        match3Id, TOURNAMENT_ID, null, null,
                        2, 1, null, null,
                        ClawgicMatchStatus.SCHEDULED, null,
                        null, NOW, NOW
                )
        );
    }
}
