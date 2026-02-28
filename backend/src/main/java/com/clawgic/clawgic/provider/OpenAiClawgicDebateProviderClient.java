package com.clawgic.clawgic.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.clawgic.clawgic.config.ClawgicProviderProperties;
import com.clawgic.clawgic.model.ClawgicProviderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Objects;

/**
 * Live OpenAI adapter for Clawgic debate turns.
 */
@Component
public class OpenAiClawgicDebateProviderClient implements ClawgicDebateProviderClient {

    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ClawgicProviderProperties clawgicProviderProperties;

    @Autowired
    public OpenAiClawgicDebateProviderClient(
            RestClient.Builder restClientBuilder,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            ClawgicProviderProperties clawgicProviderProperties
    ) {
        this(
                restClientBuilder,
                objectMapperProvider.getIfAvailable(ObjectMapper::new),
                clawgicProviderProperties
        );
    }

    OpenAiClawgicDebateProviderClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            ClawgicProviderProperties clawgicProviderProperties
    ) {
        this.objectMapper = objectMapper;
        this.clawgicProviderProperties = clawgicProviderProperties;
        this.restClient = restClientBuilder
                .clone()
                .requestFactory(newRequestFactory(clawgicProviderProperties))
                .baseUrl(clawgicProviderProperties.getOpenaiBaseUrl())
                .build();
    }

    @Override
    public ClawgicProviderType providerType() {
        return ClawgicProviderType.OPENAI;
    }

    @Override
    public ClawgicProviderTurnResponse generateTurn(ClawgicProviderTurnRequest request) {
        Objects.requireNonNull(request, "request is required");
        if (!StringUtils.hasText(request.providerApiKey())) {
            throw new IllegalStateException("OpenAI authentication key is required");
        }

        String userPrompt = ClawgicProviderPromptComposer.toUserPrompt(request);
        String responseBody = ClawgicProviderRetrySupport.execute(
                "OpenAI",
                clawgicProviderProperties.getRetryMaxAttempts(),
                clawgicProviderProperties.getRetryBackoffMs(),
                () -> executeRequest(request, userPrompt)
        );
        String content = OpenAiResponseParser.extractAssistantText(objectMapper, responseBody);
        return new ClawgicProviderTurnResponse(trimToWordLimit(content, request.maxWords()), request.model());
    }

    private String executeRequest(ClawgicProviderTurnRequest request, String userPrompt) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", request.model());
        payload.put("temperature", clawgicProviderProperties.getOpenaiTemperature());
        payload.put("max_tokens", resolveMaxTokens(request.maxWords()));

        ArrayNode messages = payload.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", request.systemPrompt());
        messages.addObject()
                .put("role", "user")
                .put("content", userPrompt);

        return restClient.post()
                .uri(CHAT_COMPLETIONS_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + request.providerApiKey())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(payload)
                .retrieve()
                .body(String.class);
    }

    private int resolveMaxTokens(int maxWords) {
        int multiplier = Math.max(1, clawgicProviderProperties.getMaxTokensPerWord());
        int minTokens = Math.max(1, clawgicProviderProperties.getMinResponseTokens());
        return Math.max(minTokens, maxWords * multiplier);
    }

    private static SimpleClientHttpRequestFactory newRequestFactory(ClawgicProviderProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.max(1, properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Math.max(1, properties.getReadTimeoutMs()));
        return factory;
    }

    private static String trimToWordLimit(String value, int maxWords) {
        String normalized = value.trim();
        String[] words = normalized.split("\\s+");
        if (words.length <= maxWords) {
            return normalized;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(words[i]);
        }
        return builder.toString();
    }
}
