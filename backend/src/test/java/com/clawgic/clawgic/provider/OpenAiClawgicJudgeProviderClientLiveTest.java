package com.clawgic.clawgic.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.clawgic.clawgic.config.ClawgicJudgeProperties;
import com.clawgic.clawgic.config.ClawgicProviderProperties;
import com.clawgic.clawgic.model.DebatePhase;
import com.clawgic.clawgic.model.DebateTranscriptMessage;
import com.clawgic.clawgic.model.DebateTranscriptRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "CLAWGIC_LIVE_JUDGE_SMOKE", matches = "true")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class OpenAiClawgicJudgeProviderClientLiveTest {

    @Test
    void evaluatesOneLiveJudgeVerdictViaOpenAiApi() {
        ClawgicProviderProperties providerProperties = new ClawgicProviderProperties();
        String baseUrlOverride = System.getenv("OPENAI_BASE_URL");
        if (StringUtils.hasText(baseUrlOverride)) {
            providerProperties.setOpenaiBaseUrl(baseUrlOverride);
        }
        providerProperties.setRetryMaxAttempts(1);
        providerProperties.setRetryBackoffMs(0);

        ClawgicJudgeProperties judgeProperties = new ClawgicJudgeProperties();
        judgeProperties.setOpenaiApiKey(System.getenv("OPENAI_API_KEY"));
        judgeProperties.setMalformedJsonMaxAttempts(1);
        judgeProperties.setStrictJson(true);
        judgeProperties.setMaxResponseTokens(512);

        String modelOverride = System.getenv("OPENAI_JUDGE_MODEL");
        String model = StringUtils.hasText(modelOverride)
                ? modelOverride.trim()
                : judgeProperties.getModel();

        OpenAiClawgicJudgeProviderClient client = new OpenAiClawgicJudgeProviderClient(
                RestClient.builder(),
                new ObjectMapper(),
                providerProperties,
                judgeProperties
        );

        UUID agent1Id = UUID.fromString("00000000-0000-0000-0000-000000000951");
        UUID agent2Id = UUID.fromString("00000000-0000-0000-0000-000000000952");
        ClawgicJudgeRequest request = new ClawgicJudgeRequest(
                UUID.fromString("00000000-0000-0000-0000-000000000953"),
                UUID.fromString("00000000-0000-0000-0000-000000000954"),
                agent1Id,
                agent2Id,
                "Should AI debate judging enforce deterministic JSON output formats?",
                List.of(
                        new DebateTranscriptMessage(
                                DebateTranscriptRole.AGENT_1,
                                DebatePhase.ARGUMENTATION,
                                "Deterministic JSON reduces integration risk and post-processing ambiguity."
                        ),
                        new DebateTranscriptMessage(
                                DebateTranscriptRole.AGENT_2,
                                DebatePhase.COUNTER_ARGUMENTATION,
                                "Flexibility can capture richer nuance, but validation must remain strict."
                        )
                ),
                "live-judge-primary",
                model
        );

        ObjectNode result = client.evaluate(request);
        assertTrue(result.has("winner_id"));
        assertTrue(result.has("agent_1"));
        assertTrue(result.has("agent_2"));
        assertTrue(result.has("reasoning"));
        assertTrue(Set.of(agent1Id.toString(), agent2Id.toString()).contains(result.path("winner_id").asText()));
    }
}
