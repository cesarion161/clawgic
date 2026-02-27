package com.moltrank.clawgic.service;

import com.moltrank.clawgic.config.ClawgicRuntimeProperties;
import com.moltrank.clawgic.dto.ClawgicTournamentRequests;
import com.moltrank.clawgic.dto.ClawgicTournamentResponses;
import com.moltrank.clawgic.model.ClawgicTournament;
import com.moltrank.clawgic.model.ClawgicTournamentStatus;
import com.moltrank.clawgic.repository.ClawgicTournamentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=${C10_TEST_DB_URL:jdbc:postgresql://localhost:5432/moltrank}",
        "spring.datasource.username=${C10_TEST_DB_USERNAME:moltrank}",
        "spring.datasource.password=${C10_TEST_DB_PASSWORD:changeme}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "moltrank.ingestion.enabled=false",
        "moltrank.ingestion.run-on-startup=false"
})
@Transactional
class ClawgicTournamentServiceIntegrationTest {

    @Autowired
    private ClawgicTournamentService clawgicTournamentService;

    @Autowired
    private ClawgicTournamentRepository clawgicTournamentRepository;

    @Autowired
    private ClawgicRuntimeProperties clawgicRuntimeProperties;

    @Test
    void createTournamentPersistsScheduledTournamentWithMvpDefaults() {
        OffsetDateTime startTime = OffsetDateTime.now().plusHours(3);
        ClawgicTournamentResponses.TournamentDetail created = clawgicTournamentService.createTournament(
                new ClawgicTournamentRequests.CreateTournamentRequest(
                        "C17 integration tournament",
                        startTime,
                        null,
                        new BigDecimal("7.250000")
                )
        );

        assertEquals(ClawgicTournamentStatus.SCHEDULED, created.status());
        assertEquals(clawgicRuntimeProperties.getTournament().getMvpBracketSize(), created.bracketSize());
        assertEquals(created.bracketSize(), created.maxEntries());
        assertEquals(
                startTime.minusMinutes(clawgicRuntimeProperties.getTournament().getDefaultEntryWindowMinutes()),
                created.entryCloseTime()
        );

        ClawgicTournament persisted = clawgicTournamentRepository.findById(created.tournamentId()).orElseThrow();
        assertEquals(new BigDecimal("7.250000"), persisted.getBaseEntryFeeUsdc());
        assertEquals(ClawgicTournamentStatus.SCHEDULED, persisted.getStatus());
    }

    @Test
    void createTournamentRejectsEntryCloseAfterStartTime() {
        OffsetDateTime startTime = OffsetDateTime.now().plusHours(4);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                clawgicTournamentService.createTournament(new ClawgicTournamentRequests.CreateTournamentRequest(
                        "Invalid entry window",
                        startTime,
                        startTime.plusMinutes(1),
                        new BigDecimal("5.000000")
                )));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("400 BAD_REQUEST \"entryCloseTime must be on or before startTime\"", ex.getMessage());
    }

    @Test
    void listUpcomingTournamentsReturnsOnlyScheduledFutureTournamentsInOrder() {
        OffsetDateTime now = OffsetDateTime.now();
        ClawgicTournament futureEarly = insertTournament(
                "future early",
                ClawgicTournamentStatus.SCHEDULED,
                now.plusHours(2),
                now.plusHours(1)
        );
        ClawgicTournament futureLate = insertTournament(
                "future late",
                ClawgicTournamentStatus.SCHEDULED,
                now.plusHours(4),
                now.plusHours(3)
        );
        insertTournament("past", ClawgicTournamentStatus.SCHEDULED, now.minusHours(1), now.minusHours(2));
        insertTournament("locked", ClawgicTournamentStatus.LOCKED, now.plusHours(5), now.plusHours(4));

        List<ClawgicTournamentResponses.TournamentSummary> upcoming =
                clawgicTournamentService.listUpcomingTournaments();

        assertEquals(2, upcoming.size());
        assertEquals(futureEarly.getTournamentId(), upcoming.get(0).tournamentId());
        assertEquals(futureLate.getTournamentId(), upcoming.get(1).tournamentId());
    }

    private ClawgicTournament insertTournament(
            String topic,
            ClawgicTournamentStatus status,
            OffsetDateTime startTime,
            OffsetDateTime entryCloseTime
    ) {
        ClawgicTournament tournament = new ClawgicTournament();
        tournament.setTournamentId(UUID.randomUUID());
        tournament.setTopic(topic);
        tournament.setStatus(status);
        tournament.setBracketSize(4);
        tournament.setMaxEntries(4);
        tournament.setStartTime(startTime);
        tournament.setEntryCloseTime(entryCloseTime);
        tournament.setBaseEntryFeeUsdc(new BigDecimal("5.000000"));
        tournament.setCreatedAt(entryCloseTime.minusMinutes(10));
        tournament.setUpdatedAt(entryCloseTime.minusMinutes(10));
        return clawgicTournamentRepository.saveAndFlush(tournament);
    }
}
