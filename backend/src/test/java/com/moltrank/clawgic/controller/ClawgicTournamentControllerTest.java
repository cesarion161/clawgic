package com.moltrank.clawgic.controller;

import com.moltrank.clawgic.dto.ClawgicTournamentResponses;
import com.moltrank.clawgic.model.ClawgicTournamentStatus;
import com.moltrank.clawgic.service.ClawgicTournamentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClawgicTournamentController.class)
class ClawgicTournamentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClawgicTournamentService clawgicTournamentService;

    @Test
    void createTournamentReturnsCreatedPayload() throws Exception {
        UUID tournamentId = UUID.fromString("00000000-0000-0000-0000-000000000401");
        when(clawgicTournamentService.createTournament(any())).thenReturn(sampleDetail(tournamentId));

        mockMvc.perform(post("/api/clawgic/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "Should judges reveal chain-of-thought?",
                                  "startTime": "2026-06-01T14:00:00Z",
                                  "entryCloseTime": "2026-06-01T13:00:00Z",
                                  "baseEntryFeeUsdc": 5.25
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tournamentId").value(tournamentId.toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.bracketSize").value(4))
                .andExpect(jsonPath("$.baseEntryFeeUsdc").value(5.25));
    }

    @Test
    void createTournamentValidationFailureReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/clawgic/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "",
                                  "startTime": "2024-01-01T00:00:00Z",
                                  "baseEntryFeeUsdc": -1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(clawgicTournamentService, never()).createTournament(any());
    }

    @Test
    void createTournamentRejectsEntryCloseAfterStartTime() throws Exception {
        mockMvc.perform(post("/api/clawgic/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "Can agents self-correct hallucinations?",
                                  "startTime": "2026-06-01T14:00:00Z",
                                  "entryCloseTime": "2026-06-01T14:30:00Z",
                                  "baseEntryFeeUsdc": 5.00
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(clawgicTournamentService, never()).createTournament(any());
    }

    @Test
    void listUpcomingTournamentsReturnsSummaries() throws Exception {
        UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000411");
        UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000412");
        when(clawgicTournamentService.listUpcomingTournaments())
                .thenReturn(List.of(sampleSummary(firstId, "Debate One"), sampleSummary(secondId, "Debate Two")));

        mockMvc.perform(get("/api/clawgic/tournaments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tournamentId").value(firstId.toString()))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$[1].tournamentId").value(secondId.toString()));
    }

    private static ClawgicTournamentResponses.TournamentDetail sampleDetail(UUID tournamentId) {
        OffsetDateTime created = OffsetDateTime.parse("2026-05-01T12:00:00Z");
        return new ClawgicTournamentResponses.TournamentDetail(
                tournamentId,
                "Should judges reveal chain-of-thought?",
                ClawgicTournamentStatus.SCHEDULED,
                4,
                4,
                OffsetDateTime.parse("2026-06-01T14:00:00Z"),
                OffsetDateTime.parse("2026-06-01T13:00:00Z"),
                new BigDecimal("5.250000"),
                null,
                created,
                created,
                null,
                null
        );
    }

    private static ClawgicTournamentResponses.TournamentSummary sampleSummary(UUID tournamentId, String topic) {
        OffsetDateTime created = OffsetDateTime.parse("2026-05-01T12:00:00Z");
        return new ClawgicTournamentResponses.TournamentSummary(
                tournamentId,
                topic,
                ClawgicTournamentStatus.SCHEDULED,
                4,
                4,
                OffsetDateTime.parse("2026-06-01T14:00:00Z"),
                OffsetDateTime.parse("2026-06-01T13:00:00Z"),
                new BigDecimal("5.000000"),
                null,
                created,
                created
        );
    }
}
