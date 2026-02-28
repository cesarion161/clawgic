package com.clawgic.clawgic.service;

import com.clawgic.clawgic.dto.ClawgicAgentRequests;
import com.clawgic.clawgic.dto.ClawgicAgentResponses;
import com.clawgic.clawgic.model.ClawgicAgent;
import com.clawgic.clawgic.model.ClawgicAgentElo;
import com.clawgic.clawgic.model.ClawgicProviderType;
import com.clawgic.clawgic.repository.ClawgicAgentEloRepository;
import com.clawgic.clawgic.repository.ClawgicAgentRepository;
import com.clawgic.clawgic.repository.ClawgicUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
})
@Transactional
class ClawgicAgentServiceIntegrationTest {

    @Autowired
    private ClawgicAgentService clawgicAgentService;

    @Autowired
    private ClawgicAgentRepository clawgicAgentRepository;

    @Autowired
    private ClawgicAgentEloRepository clawgicAgentEloRepository;

    @Autowired
    private ClawgicUserRepository clawgicUserRepository;

    @Test
    void createAgentPersistsEncryptedRecordAndInitialEloRow() {
        ClawgicAgentResponses.AgentDetail created = clawgicAgentService.createAgent(new ClawgicAgentRequests.CreateAgentRequest(
                "0x1234567890ABCDEF1234567890ABCDEF12345678",
                "C16 Integration Agent",
                "https://example.com/c16-avatar.png",
                "Debate with reproducible structure.",
                "- rebuttal",
                "Analytical",
                "# AGENTS.md\nC16",
                ClawgicProviderType.OPENAI,
                "team/openai/primary",
                "sk-live-c16-integration"
        ));

        assertNotNull(created.agentId());
        assertEquals("OPENAI", created.providerType());
        assertEquals("team/openai/primary", created.providerKeyRef());
        assertTrue(created.apiKeyConfigured());
        assertNotNull(created.elo());
        assertEquals(1000, created.elo().currentElo());

        ClawgicAgent persistedAgent = clawgicAgentRepository.findById(created.agentId()).orElseThrow();
        assertEquals("0x1234567890abcdef1234567890abcdef12345678", persistedAgent.getWalletAddress());
        assertEquals(ClawgicProviderType.OPENAI, persistedAgent.getProviderType());
        assertEquals("team/openai/primary", persistedAgent.getProviderKeyRef());
        assertFalse(persistedAgent.getApiKeyEncrypted().contains("sk-live-c16-integration"));

        ClawgicAgentElo persistedElo = clawgicAgentEloRepository.findById(created.agentId()).orElseThrow();
        assertEquals(1000, persistedElo.getCurrentElo());
        assertEquals(0, persistedElo.getMatchesPlayed());
        assertEquals(0, persistedElo.getMatchesWon());
        assertEquals(0, persistedElo.getMatchesForfeited());

        assertTrue(clawgicUserRepository.existsById("0x1234567890abcdef1234567890abcdef12345678"));
    }

    @Test
    void listAgentsFiltersByWalletAddress() {
        clawgicAgentService.createAgent(new ClawgicAgentRequests.CreateAgentRequest(
                "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "Wallet A Agent",
                null,
                "Prompt A",
                null,
                null,
                null,
                ClawgicProviderType.OPENAI,
                null,
                "sk-live-wallet-a"
        ));
        clawgicAgentService.createAgent(new ClawgicAgentRequests.CreateAgentRequest(
                "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                "Wallet B Agent",
                null,
                "Prompt B",
                null,
                null,
                null,
                ClawgicProviderType.MOCK,
                null,
                "sk-live-wallet-b"
        ));

        List<ClawgicAgentResponses.AgentSummary> walletAAgents =
                clawgicAgentService.listAgents("0xAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

        assertEquals(1, walletAAgents.size());
        assertEquals("Wallet A Agent", walletAAgents.getFirst().name());
        assertEquals("OPENAI", walletAAgents.getFirst().providerType());
    }
}
