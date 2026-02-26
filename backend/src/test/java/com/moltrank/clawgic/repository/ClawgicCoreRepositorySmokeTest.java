package com.moltrank.clawgic.repository;

import com.moltrank.clawgic.model.ClawgicAgent;
import com.moltrank.clawgic.model.ClawgicAgentElo;
import com.moltrank.clawgic.model.ClawgicUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        "moltrank.ingestion.run-on-startup=false"
})
@Transactional
class ClawgicCoreRepositorySmokeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ClawgicUserRepository clawgicUserRepository;

    @Autowired
    private ClawgicAgentRepository clawgicAgentRepository;

    @Autowired
    private ClawgicAgentEloRepository clawgicAgentEloRepository;

    @Test
    void flywayCreatesClawgicCoreTablesAndRepositoriesRoundTripRecords() {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name IN ('clawgic_users', 'clawgic_agents', 'clawgic_agent_elo')
                        """,
                Integer.class);

        assertEquals(3, tableCount);

        String walletAddress = "0x1111111111111111111111111111111111111111";
        UUID agentId = UUID.randomUUID();

        ClawgicUser user = new ClawgicUser();
        user.setWalletAddress(walletAddress);
        clawgicUserRepository.saveAndFlush(user);

        ClawgicAgent agent = new ClawgicAgent();
        agent.setAgentId(agentId);
        agent.setWalletAddress(walletAddress);
        agent.setName("Deterministic Agent");
        agent.setSystemPrompt("You are a precise debate agent.");
        agent.setSkillsMarkdown("- rebuttal\\n- logic");
        agent.setPersona("Analytical");
        agent.setAgentsMdSource("# AGENTS\\n...");
        agent.setApiKeyEncrypted("enc:placeholder-c10");
        agent.setApiKeyEncryptionKeyId("pending-encryption-step");
        clawgicAgentRepository.saveAndFlush(agent);

        ClawgicAgentElo elo = new ClawgicAgentElo();
        elo.setAgentId(agentId);
        clawgicAgentEloRepository.saveAndFlush(elo);

        assertTrue(clawgicUserRepository.findById(walletAddress).isPresent());

        List<ClawgicAgent> agentsForWallet = clawgicAgentRepository.findByWalletAddress(walletAddress);
        assertEquals(1, agentsForWallet.size());
        assertEquals(agentId, agentsForWallet.getFirst().getAgentId());

        ClawgicAgentElo persistedElo = clawgicAgentEloRepository.findById(agentId).orElseThrow();
        assertEquals(1000, persistedElo.getCurrentElo());
        assertEquals(0, persistedElo.getMatchesPlayed());
        assertEquals(0, persistedElo.getMatchesWon());
        assertEquals(0, persistedElo.getMatchesForfeited());
    }
}
