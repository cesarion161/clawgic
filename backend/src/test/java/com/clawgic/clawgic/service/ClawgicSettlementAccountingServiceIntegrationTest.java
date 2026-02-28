package com.clawgic.clawgic.service;

import com.clawgic.clawgic.model.ClawgicAgent;
import com.clawgic.clawgic.model.ClawgicMatch;
import com.clawgic.clawgic.model.ClawgicMatchStatus;
import com.clawgic.clawgic.model.ClawgicProviderType;
import com.clawgic.clawgic.model.ClawgicStakingLedger;
import com.clawgic.clawgic.model.ClawgicStakingLedgerStatus;
import com.clawgic.clawgic.model.ClawgicTournament;
import com.clawgic.clawgic.model.ClawgicTournamentEntry;
import com.clawgic.clawgic.model.ClawgicTournamentEntryStatus;
import com.clawgic.clawgic.model.ClawgicTournamentStatus;
import com.clawgic.clawgic.model.ClawgicUser;
import com.clawgic.clawgic.model.DebateTranscriptJsonCodec;
import com.clawgic.clawgic.repository.ClawgicAgentRepository;
import com.clawgic.clawgic.repository.ClawgicMatchRepository;
import com.clawgic.clawgic.repository.ClawgicStakingLedgerRepository;
import com.clawgic.clawgic.repository.ClawgicTournamentEntryRepository;
import com.clawgic.clawgic.repository.ClawgicTournamentRepository;
import com.clawgic.clawgic.repository.ClawgicUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=${C10_TEST_DB_URL:jdbc:postgresql://localhost:5432/clawgic}",
        "spring.datasource.username=${C10_TEST_DB_USERNAME:clawgic}",
        "spring.datasource.password=${C10_TEST_DB_PASSWORD:changeme}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "clawgic.tournament.judge-fee-usdc-per-completed-match=0.250000",
        "clawgic.tournament.system-retention-rate=0.050000"
})
@Transactional
class ClawgicSettlementAccountingServiceIntegrationTest {

    @Autowired
    private ClawgicSettlementAccountingService clawgicSettlementAccountingService;

    @Autowired
    private ClawgicUserRepository clawgicUserRepository;

    @Autowired
    private ClawgicAgentRepository clawgicAgentRepository;

    @Autowired
    private ClawgicTournamentRepository clawgicTournamentRepository;

    @Autowired
    private ClawgicTournamentEntryRepository clawgicTournamentEntryRepository;

    @Autowired
    private ClawgicStakingLedgerRepository clawgicStakingLedgerRepository;

    @Autowired
    private ClawgicMatchRepository clawgicMatchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void isolateClawgicTables() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    clawgic_match_judgements,
                    clawgic_matches,
                    clawgic_tournament_entries,
                    clawgic_payment_authorizations,
                    clawgic_staking_ledger,
                    clawgic_tournaments,
                    clawgic_agent_elo,
                    clawgic_agents,
                    clawgic_users
                CASCADE
                """);
    }

    @Test
    void settleTournamentIsReplaySafeAndDoesNotDoubleCreditPayouts() {
        OffsetDateTime now = OffsetDateTime.now();

        AgentFixture finalistWinner = createUserAgentAndEntry("winner", now);
        AgentFixture semifinalOneForfeitLoser = createUserAgentAndEntry("forfeit-loser", now.plusSeconds(1));
        AgentFixture semifinalTwoWinner = createUserAgentAndEntry("semi-two-winner", now.plusSeconds(2));
        AgentFixture semifinalTwoLoser = createUserAgentAndEntry("semi-two-loser", now.plusSeconds(3));

        ClawgicTournament tournament = createCompletedTournament(finalistWinner.agentId(), now);

        attachEntryAndLedger(tournament.getTournamentId(), finalistWinner, now.plusSeconds(4));
        attachEntryAndLedger(tournament.getTournamentId(), semifinalOneForfeitLoser, now.plusSeconds(5));
        attachEntryAndLedger(tournament.getTournamentId(), semifinalTwoWinner, now.plusSeconds(6));
        attachEntryAndLedger(tournament.getTournamentId(), semifinalTwoLoser, now.plusSeconds(7));

        UUID finalMatchId = UUID.randomUUID();
        createResolvedMatch(
                tournament.getTournamentId(),
                finalistWinner.agentId(),
                semifinalTwoWinner.agentId(),
                2,
                1,
                ClawgicMatchStatus.COMPLETED,
                null,
                null,
                finalistWinner.agentId(),
                now.minusMinutes(3),
                finalMatchId
        );
        createResolvedMatch(
                tournament.getTournamentId(),
                finalistWinner.agentId(),
                semifinalOneForfeitLoser.agentId(),
                1,
                1,
                ClawgicMatchStatus.FORFEITED,
                finalMatchId,
                1,
                finalistWinner.agentId(),
                now.minusMinutes(5),
                UUID.randomUUID()
        );
        createResolvedMatch(
                tournament.getTournamentId(),
                semifinalTwoWinner.agentId(),
                semifinalTwoLoser.agentId(),
                1,
                2,
                ClawgicMatchStatus.COMPLETED,
                finalMatchId,
                2,
                semifinalTwoWinner.agentId(),
                now.minusMinutes(4),
                UUID.randomUUID()
        );

        ClawgicSettlementAccountingService.SettlementResult firstSettlement =
                clawgicSettlementAccountingService.settleTournamentIfCompleted(tournament.getTournamentId(), now);
        assertTrue(firstSettlement.applied());
        assertFalse(firstSettlement.alreadySettled());
        assertEquals("APPLIED", firstSettlement.reason());
        assertEquals(new BigDecimal("20.000000"), firstSettlement.totalPool());
        assertEquals(new BigDecimal("0.500000"), firstSettlement.totalJudgeFee());
        assertEquals(new BigDecimal("0.975000"), firstSettlement.totalSystemRetention());
        assertEquals(new BigDecimal("18.525000"), firstSettlement.totalRewardPayout());

        List<ClawgicStakingLedger> settledLedgers =
                clawgicStakingLedgerRepository.findByTournamentIdOrderByCreatedAtAsc(tournament.getTournamentId());
        assertEquals(4, settledLedgers.size());
        Map<UUID, ClawgicStakingLedger> ledgerByAgentId = new LinkedHashMap<>();
        for (ClawgicStakingLedger ledger : settledLedgers) {
            ledgerByAgentId.put(ledger.getAgentId(), ledger);
            assertNotNull(ledger.getSettledAt());
        }

        ClawgicStakingLedger winnerLedger = ledgerByAgentId.get(finalistWinner.agentId());
        assertEquals(ClawgicStakingLedgerStatus.SETTLED, winnerLedger.getStatus());
        assertEquals(new BigDecimal("18.525000"), winnerLedger.getRewardPayout());

        ClawgicStakingLedger forfeitedLedger = ledgerByAgentId.get(semifinalOneForfeitLoser.agentId());
        assertEquals(ClawgicStakingLedgerStatus.FORFEITED, forfeitedLedger.getStatus());
        assertNotNull(forfeitedLedger.getForfeitedAt());
        assertEquals(new BigDecimal("0.000000"), forfeitedLedger.getRewardPayout());

        assertEquals(new BigDecimal("0.500000"), sum(settledLedgers, ClawgicStakingLedger::getJudgeFeeDeducted));
        assertEquals(new BigDecimal("0.975000"), sum(settledLedgers, ClawgicStakingLedger::getSystemRetention));
        assertEquals(new BigDecimal("18.525000"), sum(settledLedgers, ClawgicStakingLedger::getRewardPayout));

        Map<UUID, LedgerSnapshot> snapshotsAfterFirstRun = snapshot(settledLedgers);

        ClawgicSettlementAccountingService.SettlementResult secondSettlement =
                clawgicSettlementAccountingService.settleTournamentIfCompleted(
                        tournament.getTournamentId(),
                        now.plusSeconds(30)
                );
        assertFalse(secondSettlement.applied());
        assertTrue(secondSettlement.alreadySettled());
        assertEquals("ALREADY_SETTLED", secondSettlement.reason());

        List<ClawgicStakingLedger> rerunLedgers =
                clawgicStakingLedgerRepository.findByTournamentIdOrderByCreatedAtAsc(tournament.getTournamentId());
        assertEquals(snapshotsAfterFirstRun, snapshot(rerunLedgers));
    }

    private AgentFixture createUserAgentAndEntry(String seed, OffsetDateTime now) {
        String walletAddress = randomWalletAddress();
        ClawgicUser user = new ClawgicUser();
        user.setWalletAddress(walletAddress);
        user.setCreatedAt(now.minusMinutes(20));
        user.setUpdatedAt(now.minusMinutes(20));
        clawgicUserRepository.saveAndFlush(user);

        UUID agentId = UUID.randomUUID();
        ClawgicAgent agent = new ClawgicAgent();
        agent.setAgentId(agentId);
        agent.setWalletAddress(walletAddress);
        agent.setName("settlement-agent-" + seed);
        agent.setSystemPrompt("Debate with deterministic structure.");
        agent.setPersona("Prioritize correctness.");
        agent.setProviderType(ClawgicProviderType.MOCK);
        agent.setApiKeyEncrypted("enc:test");
        agent.setCreatedAt(now.minusMinutes(20));
        agent.setUpdatedAt(now.minusMinutes(20));
        clawgicAgentRepository.saveAndFlush(agent);

        return new AgentFixture(agentId, walletAddress);
    }

    private ClawgicTournament createCompletedTournament(UUID winnerAgentId, OffsetDateTime now) {
        ClawgicTournament tournament = new ClawgicTournament();
        tournament.setTournamentId(UUID.randomUUID());
        tournament.setTopic("C30 integration settlement topic");
        tournament.setStatus(ClawgicTournamentStatus.COMPLETED);
        tournament.setBracketSize(4);
        tournament.setMaxEntries(4);
        tournament.setStartTime(now.minusHours(1));
        tournament.setEntryCloseTime(now.minusHours(2));
        tournament.setBaseEntryFeeUsdc(new BigDecimal("5.000000"));
        tournament.setWinnerAgentId(winnerAgentId);
        tournament.setMatchesCompleted(2);
        tournament.setMatchesForfeited(1);
        tournament.setStartedAt(now.minusMinutes(40));
        tournament.setCompletedAt(now.minusMinutes(5));
        tournament.setCreatedAt(now.minusHours(3));
        tournament.setUpdatedAt(now.minusMinutes(5));
        return clawgicTournamentRepository.saveAndFlush(tournament);
    }

    private void attachEntryAndLedger(UUID tournamentId, AgentFixture fixture, OffsetDateTime now) {
        ClawgicTournamentEntry entry = new ClawgicTournamentEntry();
        entry.setEntryId(UUID.randomUUID());
        entry.setTournamentId(tournamentId);
        entry.setAgentId(fixture.agentId());
        entry.setWalletAddress(fixture.walletAddress());
        entry.setStatus(ClawgicTournamentEntryStatus.CONFIRMED);
        entry.setSeedSnapshotElo(1000);
        entry.setCreatedAt(now.minusMinutes(15));
        entry.setUpdatedAt(now.minusMinutes(15));
        clawgicTournamentEntryRepository.saveAndFlush(entry);

        ClawgicStakingLedger ledger = new ClawgicStakingLedger();
        ledger.setStakeId(UUID.randomUUID());
        ledger.setTournamentId(tournamentId);
        ledger.setEntryId(entry.getEntryId());
        ledger.setAgentId(fixture.agentId());
        ledger.setWalletAddress(fixture.walletAddress());
        ledger.setAmountStaked(new BigDecimal("5.000000"));
        ledger.setJudgeFeeDeducted(new BigDecimal("0.000000"));
        ledger.setSystemRetention(new BigDecimal("0.000000"));
        ledger.setRewardPayout(new BigDecimal("0.000000"));
        ledger.setStatus(ClawgicStakingLedgerStatus.ENTERED);
        ledger.setSettlementNote("BYPASS_ACCEPTED (x402.enabled=false)");
        ledger.setAuthorizedAt(now.minusMinutes(14));
        ledger.setEnteredAt(now.minusMinutes(13));
        ledger.setCreatedAt(now.minusMinutes(12));
        ledger.setUpdatedAt(now.minusMinutes(11));
        clawgicStakingLedgerRepository.saveAndFlush(ledger);
    }

    private void createResolvedMatch(
            UUID tournamentId,
            UUID agent1Id,
            UUID agent2Id,
            int bracketRound,
            int bracketPosition,
            ClawgicMatchStatus status,
            UUID nextMatchId,
            Integer nextMatchAgentSlot,
            UUID winnerAgentId,
            OffsetDateTime now,
            UUID matchId
    ) {
        ClawgicMatch match = new ClawgicMatch();
        match.setMatchId(matchId);
        match.setTournamentId(tournamentId);
        match.setAgent1Id(agent1Id);
        match.setAgent2Id(agent2Id);
        match.setBracketRound(bracketRound);
        match.setBracketPosition(bracketPosition);
        match.setNextMatchId(nextMatchId);
        match.setNextMatchAgentSlot(nextMatchAgentSlot);
        match.setStatus(status);
        match.setWinnerAgentId(winnerAgentId);
        match.setTranscriptJson(DebateTranscriptJsonCodec.emptyTranscript());
        match.setJudgeRetryCount(0);
        match.setStartedAt(now.minusMinutes(1));
        match.setCreatedAt(now.minusMinutes(2));
        match.setUpdatedAt(now.minusMinutes(1));

        if (status == ClawgicMatchStatus.COMPLETED) {
            match.setJudgedAt(now);
            match.setCompletedAt(now);
        } else if (status == ClawgicMatchStatus.FORFEITED) {
            match.setForfeitReason("TEST_FORFEIT");
            match.setForfeitedAt(now);
        }

        clawgicMatchRepository.saveAndFlush(match);
    }

    private static BigDecimal sum(
            List<ClawgicStakingLedger> ledgers,
            java.util.function.Function<ClawgicStakingLedger, BigDecimal> valueExtractor
    ) {
        BigDecimal total = BigDecimal.ZERO.setScale(6);
        for (ClawgicStakingLedger ledger : ledgers) {
            total = total.add(valueExtractor.apply(ledger));
        }
        return total.setScale(6);
    }

    private static Map<UUID, LedgerSnapshot> snapshot(List<ClawgicStakingLedger> ledgers) {
        Map<UUID, LedgerSnapshot> byStakeId = new LinkedHashMap<>();
        for (ClawgicStakingLedger ledger : ledgers) {
            byStakeId.put(
                    ledger.getStakeId(),
                    new LedgerSnapshot(
                            ledger.getJudgeFeeDeducted(),
                            ledger.getSystemRetention(),
                            ledger.getRewardPayout(),
                            ledger.getStatus(),
                            ledger.getForfeitedAt(),
                            ledger.getSettledAt()
                    )
            );
        }
        return byStakeId;
    }

    private static String randomWalletAddress() {
        String hex = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        return "0x" + hex.substring(0, 40);
    }

    private record AgentFixture(UUID agentId, String walletAddress) {
    }

    private record LedgerSnapshot(
            BigDecimal judgeFeeDeducted,
            BigDecimal systemRetention,
            BigDecimal rewardPayout,
            ClawgicStakingLedgerStatus status,
            OffsetDateTime forfeitedAt,
            OffsetDateTime settledAt
    ) {
    }
}
