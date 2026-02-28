package com.clawgic.clawgic.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.clawgic.clawgic.config.ClawgicJudgeProperties;
import com.clawgic.clawgic.config.ClawgicRuntimeProperties;
import com.clawgic.clawgic.model.ClawgicMatch;
import com.clawgic.clawgic.model.ClawgicTournament;
import com.clawgic.clawgic.model.DebateTranscriptMessage;
import com.clawgic.clawgic.provider.ClawgicJudgeRequest;
import com.clawgic.clawgic.provider.MockClawgicJudgeProviderClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * Resolves judge-provider selection and executes one judge attempt.
 */
@Service
@RequiredArgsConstructor
public class ClawgicJudgeProviderGateway {

    private final ClawgicRuntimeProperties clawgicRuntimeProperties;
    private final ClawgicJudgeProperties clawgicJudgeProperties;
    private final MockClawgicJudgeProviderClient mockClawgicJudgeProviderClient;

    public JudgeEvaluation evaluate(
            ClawgicMatch match,
            ClawgicTournament tournament,
            String judgeKey,
            List<DebateTranscriptMessage> transcript
    ) {
        Objects.requireNonNull(match, "match is required");
        Objects.requireNonNull(tournament, "tournament is required");
        Objects.requireNonNull(transcript, "transcript is required");

        String model = resolveModel();
        String topic = StringUtils.hasText(tournament.getTopic()) ? tournament.getTopic().trim() : "Unknown topic";
        ClawgicJudgeRequest request = new ClawgicJudgeRequest(
                match.getMatchId(),
                tournament.getTournamentId(),
                match.getAgent1Id(),
                match.getAgent2Id(),
                topic,
                transcript,
                judgeKey,
                model
        );

        if (!clawgicRuntimeProperties.isMockJudge()) {
            throw new IllegalStateException("Live judge mode is not implemented; enable clawgic.mock-judge");
        }

        ObjectNode resultJson = mockClawgicJudgeProviderClient.evaluate(request);
        return new JudgeEvaluation(resultJson, model);
    }

    private String resolveModel() {
        if (!StringUtils.hasText(clawgicJudgeProperties.getModel())) {
            throw new IllegalStateException("clawgic.judge.model must not be blank");
        }
        return clawgicJudgeProperties.getModel().trim();
    }

    public record JudgeEvaluation(
            ObjectNode resultJson,
            String model
    ) {
    }
}
