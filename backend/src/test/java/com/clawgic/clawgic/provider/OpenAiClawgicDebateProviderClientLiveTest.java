package com.clawgic.clawgic.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.clawgic.clawgic.config.ClawgicProviderProperties;
import com.clawgic.clawgic.model.DebatePhase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "CLAWGIC_LIVE_PROVIDER_SMOKE", matches = "true")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class OpenAiClawgicDebateProviderClientLiveTest {

    @Test
    void generatesOneLiveTurnViaOpenAiApi() {
        ClawgicProviderProperties providerProperties = new ClawgicProviderProperties();
        String baseUrlOverride = System.getenv("OPENAI_BASE_URL");
        if (StringUtils.hasText(baseUrlOverride)) {
            providerProperties.setOpenaiBaseUrl(baseUrlOverride);
        }
        providerProperties.setRetryMaxAttempts(1);
        providerProperties.setRetryBackoffMs(0);

        OpenAiClawgicDebateProviderClient client = new OpenAiClawgicDebateProviderClient(
                RestClient.builder(),
                new ObjectMapper(),
                providerProperties
        );

        String modelOverride = System.getenv("OPENAI_MODEL");
        String model = StringUtils.hasText(modelOverride)
                ? modelOverride.trim()
                : providerProperties.getOpenaiDefaultModel();

        ClawgicProviderTurnRequest request = new ClawgicProviderTurnRequest(
                UUID.fromString("00000000-0000-0000-0000-000000000901"),
                UUID.fromString("00000000-0000-0000-0000-000000000902"),
                DebatePhase.ARGUMENTATION,
                "Should deterministic mocks still be mandatory in CI when live providers are available?",
                "Argue in concise and falsifiable claims.",
                List.of(),
                70,
                model,
                System.getenv("OPENAI_API_KEY"),
                "live/openai/smoke"
        );

        ClawgicProviderTurnResponse response = client.generateTurn(request);

        assertTrue(StringUtils.hasText(response.content()));
        assertEquals(model, response.model());
    }
}

