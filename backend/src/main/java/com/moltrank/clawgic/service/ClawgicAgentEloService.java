package com.moltrank.clawgic.service;

import com.moltrank.clawgic.model.ClawgicAgentElo;
import com.moltrank.clawgic.repository.ClawgicAgentEloRepository;
import com.moltrank.service.EloRatingCalculator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClawgicAgentEloService {

    private static final Logger log = LoggerFactory.getLogger(ClawgicAgentEloService.class);
    private static final int DEFAULT_INITIAL_ELO = 1000;
    private static final double MVP_K_FACTOR = 32.0d;

    private final ClawgicAgentEloRepository clawgicAgentEloRepository;

    @Transactional
    public EloUpdateResult applyJudgedMatchResult(
            UUID matchId,
            UUID agent1Id,
            UUID agent2Id,
            UUID winnerAgentId
    ) {
        validateParticipants(matchId, agent1Id, agent2Id, winnerAgentId);
        UUID loserAgentId = winnerAgentId.equals(agent1Id) ? agent2Id : agent1Id;
        OffsetDateTime now = OffsetDateTime.now();

        ClawgicAgentElo winnerElo = loadOrInitialize(winnerAgentId, now);
        ClawgicAgentElo loserElo = loadOrInitialize(loserAgentId, now);

        int winnerBefore = normalizeCurrentElo(winnerElo);
        int loserBefore = normalizeCurrentElo(loserElo);
        EloRatingCalculator.RatingPair updatedRatings = EloRatingCalculator.calculateRatings(
                winnerBefore,
                loserBefore,
                1.0d,
                0.0d,
                MVP_K_FACTOR
        );

        winnerElo.setCurrentElo(updatedRatings.playerOneRating());
        winnerElo.setMatchesPlayed(incrementCounter(winnerElo.getMatchesPlayed()));
        winnerElo.setMatchesWon(incrementCounter(winnerElo.getMatchesWon()));
        winnerElo.setLastUpdated(now);

        loserElo.setCurrentElo(updatedRatings.playerTwoRating());
        loserElo.setMatchesPlayed(incrementCounter(loserElo.getMatchesPlayed()));
        loserElo.setLastUpdated(now);

        clawgicAgentEloRepository.save(winnerElo);
        clawgicAgentEloRepository.save(loserElo);

        log.info(
                "Updated Clawgic Elo for match {}: winner {} ({} -> {}), loser {} ({} -> {}), kFactor={}",
                matchId,
                winnerAgentId,
                winnerBefore,
                winnerElo.getCurrentElo(),
                loserAgentId,
                loserBefore,
                loserElo.getCurrentElo(),
                MVP_K_FACTOR
        );

        return new EloUpdateResult(
                winnerAgentId,
                loserAgentId,
                winnerBefore,
                winnerElo.getCurrentElo(),
                loserBefore,
                loserElo.getCurrentElo()
        );
    }

    private ClawgicAgentElo loadOrInitialize(UUID agentId, OffsetDateTime now) {
        return clawgicAgentEloRepository.findByAgentIdForUpdate(agentId)
                .orElseGet(() -> initializeEloRow(agentId, now));
    }

    private ClawgicAgentElo initializeEloRow(UUID agentId, OffsetDateTime now) {
        ClawgicAgentElo agentElo = new ClawgicAgentElo();
        agentElo.setAgentId(agentId);
        agentElo.setCurrentElo(DEFAULT_INITIAL_ELO);
        agentElo.setMatchesPlayed(0);
        agentElo.setMatchesWon(0);
        agentElo.setMatchesForfeited(0);
        agentElo.setLastUpdated(now);
        return agentElo;
    }

    private static int normalizeCurrentElo(ClawgicAgentElo agentElo) {
        Integer currentElo = agentElo.getCurrentElo();
        return currentElo == null ? DEFAULT_INITIAL_ELO : currentElo;
    }

    private static int incrementCounter(Integer counterValue) {
        return counterValue == null ? 1 : counterValue + 1;
    }

    private static void validateParticipants(
            UUID matchId,
            UUID agent1Id,
            UUID agent2Id,
            UUID winnerAgentId
    ) {
        if (agent1Id == null || agent2Id == null || winnerAgentId == null) {
            throw new IllegalStateException("Match participants and winner must be set before Elo update: " + matchId);
        }
        if (agent1Id.equals(agent2Id)) {
            throw new IllegalStateException("Match participants must be distinct for Elo update: " + matchId);
        }
        if (!winnerAgentId.equals(agent1Id) && !winnerAgentId.equals(agent2Id)) {
            throw new IllegalStateException("Winner is not part of the match for Elo update: " + matchId);
        }
    }

    public record EloUpdateResult(
            UUID winnerAgentId,
            UUID loserAgentId,
            int winnerRatingBefore,
            int winnerRatingAfter,
            int loserRatingBefore,
            int loserRatingAfter
    ) {
    }
}
