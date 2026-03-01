package com.clawgic.clawgic.service;

import com.clawgic.clawgic.dto.ClawgicMatchResponses;
import com.clawgic.clawgic.dto.ClawgicTournamentResponses;
import com.clawgic.clawgic.mapper.ClawgicResponseMapper;
import com.clawgic.clawgic.model.ClawgicMatch;
import com.clawgic.clawgic.model.ClawgicMatchJudgement;
import com.clawgic.clawgic.model.ClawgicMatchStatus;
import com.clawgic.clawgic.model.ClawgicTournament;
import com.clawgic.clawgic.repository.ClawgicMatchJudgementRepository;
import com.clawgic.clawgic.repository.ClawgicMatchRepository;
import com.clawgic.clawgic.repository.ClawgicTournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClawgicMatchService {

    private final ClawgicMatchRepository clawgicMatchRepository;
    private final ClawgicMatchJudgementRepository clawgicMatchJudgementRepository;
    private final ClawgicTournamentRepository clawgicTournamentRepository;
    private final ClawgicResponseMapper clawgicResponseMapper;

    @Transactional(readOnly = true)
    public ClawgicMatchResponses.MatchDetail getMatchDetail(UUID matchId) {
        ClawgicMatch match = clawgicMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Match not found: " + matchId
                ));

        List<ClawgicMatchJudgement> judgements = clawgicMatchJudgementRepository
                .findByMatchIdOrderByCreatedAtAsc(matchId);

        return clawgicResponseMapper.toMatchDetailResponse(match, judgements);
    }

    @Transactional(readOnly = true)
    public List<ClawgicMatchResponses.MatchSummary> listTournamentMatches(UUID tournamentId) {
        if (!clawgicTournamentRepository.existsById(tournamentId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tournament not found: " + tournamentId
            );
        }

        List<ClawgicMatch> matches = clawgicMatchRepository
                .findByTournamentIdOrderByBracketRoundAscBracketPositionAscCreatedAtAsc(tournamentId);

        return clawgicResponseMapper.toMatchSummaryResponses(matches);
    }

    @Transactional(readOnly = true)
    public ClawgicTournamentResponses.TournamentLiveStatus getTournamentLiveStatus(UUID tournamentId) {
        ClawgicTournament tournament = clawgicTournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tournament not found: " + tournamentId
                ));

        List<ClawgicMatch> matches = clawgicMatchRepository
                .findByTournamentIdOrderByBracketRoundAscBracketPositionAscCreatedAtAsc(tournamentId);

        UUID activeMatchId = matches.stream()
                .filter(m -> m.getStatus() == ClawgicMatchStatus.IN_PROGRESS)
                .map(ClawgicMatch::getMatchId)
                .findFirst()
                .orElse(null);

        return new ClawgicTournamentResponses.TournamentLiveStatus(
                tournament.getTournamentId(),
                tournament.getTopic(),
                tournament.getStatus(),
                tournament.getStartTime(),
                tournament.getEntryCloseTime(),
                OffsetDateTime.now(),
                activeMatchId,
                tournament.getWinnerAgentId(),
                tournament.getMatchesCompleted(),
                tournament.getMatchesForfeited(),
                clawgicResponseMapper.toBracketMatchStatusResponses(matches)
        );
    }
}
