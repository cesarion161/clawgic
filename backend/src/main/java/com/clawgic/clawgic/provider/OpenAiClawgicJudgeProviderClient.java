package com.clawgic.clawgic.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.clawgic.clawgic.config.ClawgicJudgeProperties;
import com.clawgic.clawgic.config.ClawgicProviderProperties;
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
 * Live OpenAI adapter for Clawgic judge verdict generation.
 */
@Component
public class OpenAiClawgicJudgeProviderClient implements ClawgicJudgeProviderClient {

    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
    private static final int MAX_FALLBACK_REASONING_LENGTH = 360;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ClawgicProviderProperties clawgicProviderProperties;
    private final ClawgicJudgeProperties clawgicJudgeProperties;

    @Autowired
    public OpenAiClawgicJudgeProviderClient(
            RestClient.Builder restClientBuilder,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            ClawgicProviderProperties clawgicProviderProperties,
            ClawgicJudgeProperties clawgicJudgeProperties
    ) {
        this(
                restClientBuilder,
                objectMapperProvider.getIfAvailable(ObjectMapper::new),
                clawgicProviderProperties,
                clawgicJudgeProperties
        );
    }

    OpenAiClawgicJudgeProviderClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            ClawgicProviderProperties clawgicProviderProperties,
            ClawgicJudgeProperties clawgicJudgeProperties
    ) {
        this.objectMapper = objectMapper;
        this.clawgicProviderProperties = clawgicProviderProperties;
        this.clawgicJudgeProperties = clawgicJudgeProperties;
        this.restClient = restClientBuilder
                .clone()
                .requestFactory(newRequestFactory(clawgicProviderProperties, clawgicJudgeProperties))
                .baseUrl(clawgicProviderProperties.getOpenaiBaseUrl())
                .build();
    }

    @Override
    public ObjectNode evaluate(ClawgicJudgeRequest request) {
        Objects.requireNonNull(request, "request is required");
        String apiKey = resolveApiKey();

        int parseAttempts = Math.max(1, clawgicJudgeProperties.getMalformedJsonMaxAttempts());
        for (int parseAttempt = 1; parseAttempt <= parseAttempts; parseAttempt++) {
            String responseBody = ClawgicProviderRetrySupport.execute(
                    "OpenAI judge",
                    clawgicProviderProperties.getRetryMaxAttempts(),
                    clawgicProviderProperties.getRetryBackoffMs(),
                    () -> executeRequest(request, apiKey)
            );

            try {
                OpenAiJudgeResultParser.ParseResult parseResult =
                        OpenAiJudgeResultParser.extractJudgeResult(objectMapper, responseBody);
                return parseResult.judgeResult();
            } catch (IllegalArgumentException ex) {
                if (parseAttempt >= parseAttempts) {
                    return buildMalformedFallbackResult(request, ex, responseBody);
                }
            }
        }

        throw new IllegalStateException("Unreachable parse-attempt flow");
    }

    private String executeRequest(ClawgicJudgeRequest request, String apiKey) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", request.model());
        payload.put("temperature", 0.0d);
        payload.put("max_tokens", Math.max(128, clawgicJudgeProperties.getMaxResponseTokens()));

        if (clawgicJudgeProperties.isStrictJson()) {
            payload.set("response_format", strictJsonSchemaResponseFormat());
        }

        ArrayNode messages = payload.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", JudgePromptComposer.systemPrompt(request));
        messages.addObject()
                .put("role", "user")
                .put("content", JudgePromptComposer.userPrompt(request));

        return restClient.post()
                .uri(CHAT_COMPLETIONS_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(payload)
                .retrieve()
                .body(String.class);
    }

    private ObjectNode buildMalformedFallbackResult(
            ClawgicJudgeRequest request,
            IllegalArgumentException parseFailure,
            String responseBody
    ) {
        String errorMessage = parseFailure.getMessage() == null ? "unknown parse failure" : parseFailure.getMessage().trim();
        String summarizedBody = summarize(responseBody);
        String reasoning = "OpenAI judge response parse failed after "
                + Math.max(1, clawgicJudgeProperties.getMalformedJsonMaxAttempts())
                + " attempt(s): "
                + errorMessage
                + ". response="
                + summarizedBody;
        if (reasoning.length() > MAX_FALLBACK_REASONING_LENGTH) {
            reasoning = reasoning.substring(0, MAX_FALLBACK_REASONING_LENGTH);
        }

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("winner_id", "00000000-0000-0000-0000-000000000000");

        ObjectNode agent1 = root.putObject("agent_1");
        agent1.put("logic", 0);
        agent1.put("persona_adherence", 0);
        agent1.put("rebuttal_strength", 0);

        ObjectNode agent2 = root.putObject("agent_2");
        agent2.put("logic", 0);
        agent2.put("persona_adherence", 0);
        agent2.put("rebuttal_strength", 0);

        root.put("reasoning", reasoning);
        return root;
    }

    private ObjectNode strictJsonSchemaResponseFormat() {
        ObjectNode responseFormat = JsonNodeFactory.instance.objectNode();
        responseFormat.put("type", "json_schema");

        ObjectNode jsonSchema = responseFormat.putObject("json_schema");
        jsonSchema.put("name", "clawgic_judge_result");
        jsonSchema.put("strict", true);

        ObjectNode schema = jsonSchema.putObject("schema");
        schema.put("type", "object");
        ArrayNode required = schema.putArray("required");
        required.add("winner_id");
        required.add("agent_1");
        required.add("agent_2");
        required.add("reasoning");
        schema.put("additionalProperties", false);

        ObjectNode properties = schema.putObject("properties");
        properties.putObject("winner_id")
                .put("type", "string")
                .put("format", "uuid");
        properties.set("agent_1", scoreSchema());
        properties.set("agent_2", scoreSchema());
        properties.putObject("reasoning")
                .put("type", "string")
                .put("minLength", 1)
                .put("maxLength", 500);
        return responseFormat;
    }

    private static ObjectNode scoreSchema() {
        ObjectNode score = JsonNodeFactory.instance.objectNode();
        score.put("type", "object");
        score.put("additionalProperties", false);
        ArrayNode required = score.putArray("required");
        required.add("logic");
        required.add("persona_adherence");
        required.add("rebuttal_strength");

        ObjectNode properties = score.putObject("properties");
        properties.putObject("logic").put("type", "integer").put("minimum", 0).put("maximum", 10);
        properties.putObject("persona_adherence").put("type", "integer").put("minimum", 0).put("maximum", 10);
        properties.putObject("rebuttal_strength").put("type", "integer").put("minimum", 0).put("maximum", 10);
        return score;
    }

    private String resolveApiKey() {
        if (!StringUtils.hasText(clawgicJudgeProperties.getOpenaiApiKey())) {
            throw new IllegalStateException("clawgic.judge.openai-api-key must be configured when mock judge mode is disabled");
        }
        return clawgicJudgeProperties.getOpenaiApiKey().trim();
    }

    private static SimpleClientHttpRequestFactory newRequestFactory(
            ClawgicProviderProperties providerProperties,
            ClawgicJudgeProperties judgeProperties
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.max(1, providerProperties.getConnectTimeoutMs()));
        int readTimeoutMs = Math.max(1_000, judgeProperties.getTimeoutSeconds() * 1_000);
        factory.setReadTimeout(readTimeoutMs);
        return factory;
    }

    private static String summarize(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            return "<empty>";
        }
        String normalized = rawResponse.trim().replaceAll("\\s+", " ");
        return normalized.length() > 200 ? normalized.substring(0, 200) + "..." : normalized;
    }

    private static final class JudgePromptComposer {

        private JudgePromptComposer() {
        }

        private static String systemPrompt(ClawgicJudgeRequest request) {
            return """
                    You are the Clawgic match judge.
                    Return exactly one JSON object with these fields only:
                    - winner_id (UUID, must be one of the two competitors)
                    - agent_1.logic (integer 0..10)
                    - agent_1.persona_adherence (integer 0..10)
                    - agent_1.rebuttal_strength (integer 0..10)
                    - agent_2.logic (integer 0..10)
                    - agent_2.persona_adherence (integer 0..10)
                    - agent_2.rebuttal_strength (integer 0..10)
                    - reasoning (short string <= 500 chars)
                    Never include markdown, prose wrappers, or additional keys.
                    Match id: %s
                    Agent 1 id: %s
                    Agent 2 id: %s
                    """.formatted(request.matchId(), request.agent1Id(), request.agent2Id());
        }

        private static String userPrompt(ClawgicJudgeRequest request) {
            StringBuilder prompt = new StringBuilder();
            prompt.append("Topic: ").append(request.topic()).append('\n');
            prompt.append("Transcript:\n");
            for (int i = 0; i < request.transcript().size(); i++) {
                var message = request.transcript().get(i);
                prompt.append(i + 1)
                        .append(". [")
                        .append(message.role())
                        .append("][")
                        .append(message.phase())
                        .append("] ")
                        .append(message.content())
                        .append('\n');
            }
            prompt.append("""
                    Decide a winner based on logic quality, persona adherence, and rebuttal strength.
                    Return JSON only.
                    """);
            return prompt.toString().trim();
        }
    }
}
