package com.moltrank.clawgic.service;

import com.moltrank.clawgic.dto.ClawgicTournamentRequests;
import com.moltrank.clawgic.model.ClawgicAgent;
import com.moltrank.clawgic.model.ClawgicPaymentAuthorization;
import com.moltrank.clawgic.model.ClawgicPaymentAuthorizationStatus;
import com.moltrank.clawgic.model.ClawgicProviderType;
import com.moltrank.clawgic.model.ClawgicTournament;
import com.moltrank.clawgic.model.ClawgicTournamentStatus;
import com.moltrank.clawgic.model.ClawgicUser;
import com.moltrank.clawgic.repository.ClawgicAgentRepository;
import com.moltrank.clawgic.repository.ClawgicPaymentAuthorizationRepository;
import com.moltrank.clawgic.repository.ClawgicTournamentEntryRepository;
import com.moltrank.clawgic.repository.ClawgicTournamentRepository;
import com.moltrank.clawgic.repository.ClawgicUserRepository;
import com.moltrank.clawgic.web.X402PaymentRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=${C10_TEST_DB_URL:jdbc:postgresql://localhost:5432/moltrank}",
        "spring.datasource.username=${C10_TEST_DB_USERNAME:moltrank}",
        "spring.datasource.password=${C10_TEST_DB_PASSWORD:changeme}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "moltrank.ingestion.enabled=false",
        "moltrank.ingestion.run-on-startup=false",
        "x402.enabled=true",
        "x402.dev-bypass-enabled=false"
})
@Transactional
class ClawgicTournamentX402AuthorizationIntegrationTest {

    @Autowired
    private ClawgicTournamentService clawgicTournamentService;

    @Autowired
    private ClawgicTournamentRepository clawgicTournamentRepository;

    @Autowired
    private ClawgicUserRepository clawgicUserRepository;

    @Autowired
    private ClawgicAgentRepository clawgicAgentRepository;

    @Autowired
    private ClawgicTournamentEntryRepository clawgicTournamentEntryRepository;

    @Autowired
    private ClawgicPaymentAuthorizationRepository clawgicPaymentAuthorizationRepository;

    @Test
    void enterTournamentWithValidX402HeaderPersistsPendingAuthorizationRecord() {
        OffsetDateTime now = OffsetDateTime.now();
        String walletAddress = "0x9999999999999999999999999999999999999901";
        createUser(walletAddress);
        UUID agentId = createAgent(walletAddress, "x402 accepted");
        ClawgicTournament tournament = insertTournament(
                "C32 accepted auth record",
                now.plusHours(2),
                now.plusHours(1)
        );

        X402PaymentRequestException ex = assertThrows(X402PaymentRequestException.class, () ->
                clawgicTournamentService.enterTournament(
                        tournament.getTournamentId(),
                        new ClawgicTournamentRequests.EnterTournamentRequest(agentId),
                        """
                                {
                                  "requestNonce": "req-c32-accepted-01",
                                  "idempotencyKey": "idem-c32-accepted-01",
                                  "payload": {
                                    "authorizationNonce": "auth-c32-accepted-01",
                                    "amountUsdc": "5.000000",
                                    "chainId": 84532,
                                    "recipient": "0x000000000000000000000000000000000000c320"
                                  }
                                }
                                """
                )
        );

        assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatus());
        assertEquals("x402_verification_pending", ex.getCode());

        List<ClawgicPaymentAuthorization> authorizations =
                clawgicPaymentAuthorizationRepository.findByTournamentIdOrderByCreatedAtAsc(tournament.getTournamentId());
        assertEquals(1, authorizations.size());
        ClawgicPaymentAuthorization authorization = authorizations.getFirst();
        assertEquals(ClawgicPaymentAuthorizationStatus.PENDING_VERIFICATION, authorization.getStatus());
        assertEquals("req-c32-accepted-01", authorization.getRequestNonce());
        assertEquals("idem-c32-accepted-01", authorization.getIdempotencyKey());
        assertEquals("auth-c32-accepted-01", authorization.getAuthorizationNonce());
        assertEquals(new BigDecimal("5.000000"), authorization.getAmountAuthorizedUsdc());
        assertEquals(84532L, authorization.getChainId());
        assertEquals("0x000000000000000000000000000000000000c320", authorization.getRecipientWalletAddress());
        assertEquals(0, clawgicTournamentEntryRepository.findByTournamentIdOrderByCreatedAtAsc(tournament.getTournamentId()).size());
    }

    @Test
    void enterTournamentRejectsMalformedX402Header() {
        OffsetDateTime now = OffsetDateTime.now();
        String walletAddress = "0x9999999999999999999999999999999999999902";
        createUser(walletAddress);
        UUID agentId = createAgent(walletAddress, "x402 malformed");
        ClawgicTournament tournament = insertTournament(
                "C32 malformed payload",
                now.plusHours(2),
                now.plusHours(1)
        );

        X402PaymentRequestException ex = assertThrows(X402PaymentRequestException.class, () ->
                clawgicTournamentService.enterTournament(
                        tournament.getTournamentId(),
                        new ClawgicTournamentRequests.EnterTournamentRequest(agentId),
                        "not-json"
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("x402_malformed_payment_header", ex.getCode());
        assertTrue(clawgicPaymentAuthorizationRepository
                .findByTournamentIdOrderByCreatedAtAsc(tournament.getTournamentId())
                .isEmpty());
    }

    @Test
    void enterTournamentRejectsDuplicateRequestNonceReplay() {
        OffsetDateTime now = OffsetDateTime.now();
        String walletAddress = "0x9999999999999999999999999999999999999903";
        createUser(walletAddress);
        UUID agentId = createAgent(walletAddress, "x402 duplicate nonce");
        ClawgicTournament tournament = insertTournament(
                "C32 duplicate nonce replay",
                now.plusHours(2),
                now.plusHours(1)
        );

        assertThrows(X402PaymentRequestException.class, () ->
                clawgicTournamentService.enterTournament(
                        tournament.getTournamentId(),
                        new ClawgicTournamentRequests.EnterTournamentRequest(agentId),
                        """
                                {
                                  "requestNonce": "req-c32-dup-nonce-01",
                                  "idempotencyKey": "idem-c32-dup-nonce-01"
                                }
                                """
                )
        );

        X402PaymentRequestException replay = assertThrows(X402PaymentRequestException.class, () ->
                clawgicTournamentService.enterTournament(
                        tournament.getTournamentId(),
                        new ClawgicTournamentRequests.EnterTournamentRequest(agentId),
                        """
                                {
                                  "requestNonce": "req-c32-dup-nonce-01",
                                  "idempotencyKey": "idem-c32-dup-nonce-02"
                                }
                                """
                )
        );

        assertEquals(HttpStatus.CONFLICT, replay.getStatus());
        assertEquals("x402_replay_rejected", replay.getCode());
        assertEquals(1, clawgicPaymentAuthorizationRepository
                .findByTournamentIdOrderByCreatedAtAsc(tournament.getTournamentId())
                .size());
    }

    @Test
    void enterTournamentRejectsDuplicateIdempotencyKeyReplay() {
        OffsetDateTime now = OffsetDateTime.now();
        String walletAddress = "0x9999999999999999999999999999999999999904";
        createUser(walletAddress);
        UUID agentId = createAgent(walletAddress, "x402 duplicate idempotency");
        ClawgicTournament tournament = insertTournament(
                "C32 duplicate idempotency replay",
                now.plusHours(2),
                now.plusHours(1)
        );

        assertThrows(X402PaymentRequestException.class, () ->
                clawgicTournamentService.enterTournament(
                        tournament.getTournamentId(),
                        new ClawgicTournamentRequests.EnterTournamentRequest(agentId),
                        """
                                {
                                  "requestNonce": "req-c32-dup-idem-01",
                                  "idempotencyKey": "idem-c32-dup-idem"
                                }
                                """
                )
        );

        X402PaymentRequestException replay = assertThrows(X402PaymentRequestException.class, () ->
                clawgicTournamentService.enterTournament(
                        tournament.getTournamentId(),
                        new ClawgicTournamentRequests.EnterTournamentRequest(agentId),
                        """
                                {
                                  "requestNonce": "req-c32-dup-idem-02",
                                  "idempotencyKey": "idem-c32-dup-idem"
                                }
                                """
                )
        );

        assertEquals(HttpStatus.CONFLICT, replay.getStatus());
        assertEquals("x402_replay_rejected", replay.getCode());
        assertEquals(1, clawgicPaymentAuthorizationRepository
                .findByTournamentIdOrderByCreatedAtAsc(tournament.getTournamentId())
                .size());
    }

    private void createUser(String walletAddress) {
        ClawgicUser user = new ClawgicUser();
        user.setWalletAddress(walletAddress);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        clawgicUserRepository.saveAndFlush(user);
    }

    private UUID createAgent(String walletAddress, String name) {
        UUID agentId = UUID.randomUUID();
        ClawgicAgent agent = new ClawgicAgent();
        agent.setAgentId(agentId);
        agent.setWalletAddress(walletAddress);
        agent.setName(name);
        agent.setSystemPrompt("Debate with concise deterministic structure.");
        agent.setApiKeyEncrypted("enc:test-key");
        agent.setProviderType(ClawgicProviderType.OPENAI);
        agent.setCreatedAt(OffsetDateTime.now());
        agent.setUpdatedAt(OffsetDateTime.now());
        clawgicAgentRepository.saveAndFlush(agent);
        return agentId;
    }

    private ClawgicTournament insertTournament(
            String topic,
            OffsetDateTime startTime,
            OffsetDateTime entryCloseTime
    ) {
        ClawgicTournament tournament = new ClawgicTournament();
        tournament.setTournamentId(UUID.randomUUID());
        tournament.setTopic(topic);
        tournament.setStatus(ClawgicTournamentStatus.SCHEDULED);
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
