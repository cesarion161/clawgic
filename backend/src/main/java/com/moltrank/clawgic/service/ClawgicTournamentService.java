package com.moltrank.clawgic.service;

import com.moltrank.clawgic.config.ClawgicRuntimeProperties;
import com.moltrank.clawgic.dto.ClawgicTournamentRequests;
import com.moltrank.clawgic.dto.ClawgicTournamentResponses;
import com.moltrank.clawgic.mapper.ClawgicResponseMapper;
import com.moltrank.clawgic.model.ClawgicTournament;
import com.moltrank.clawgic.model.ClawgicTournamentStatus;
import com.moltrank.clawgic.repository.ClawgicTournamentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ClawgicTournamentService {

    private final ClawgicTournamentRepository clawgicTournamentRepository;
    private final ClawgicResponseMapper clawgicResponseMapper;
    private final ClawgicRuntimeProperties clawgicRuntimeProperties;

    public ClawgicTournamentService(
            ClawgicTournamentRepository clawgicTournamentRepository,
            ClawgicResponseMapper clawgicResponseMapper,
            ClawgicRuntimeProperties clawgicRuntimeProperties
    ) {
        this.clawgicTournamentRepository = clawgicTournamentRepository;
        this.clawgicResponseMapper = clawgicResponseMapper;
        this.clawgicRuntimeProperties = clawgicRuntimeProperties;
    }

    @Transactional
    public ClawgicTournamentResponses.TournamentDetail createTournament(
            ClawgicTournamentRequests.CreateTournamentRequest request
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startTime = request.startTime();
        OffsetDateTime entryCloseTime = resolveEntryCloseTime(startTime, request.entryCloseTime());

        if (!entryCloseTime.isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "entryCloseTime must be in the future"
            );
        }

        int bracketSize = clawgicRuntimeProperties.getTournament().getMvpBracketSize();
        if (bracketSize <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "clawgic.tournament.mvp-bracket-size must be greater than 1"
            );
        }

        ClawgicTournament tournament = new ClawgicTournament();
        tournament.setTournamentId(UUID.randomUUID());
        tournament.setTopic(request.topic().trim());
        tournament.setStatus(ClawgicTournamentStatus.SCHEDULED);
        tournament.setBracketSize(bracketSize);
        tournament.setMaxEntries(bracketSize);
        tournament.setStartTime(startTime);
        tournament.setEntryCloseTime(entryCloseTime);
        tournament.setBaseEntryFeeUsdc(request.baseEntryFeeUsdc().setScale(6, RoundingMode.HALF_UP));
        tournament.setCreatedAt(now);
        tournament.setUpdatedAt(now);

        ClawgicTournament savedTournament = clawgicTournamentRepository.save(tournament);
        return clawgicResponseMapper.toTournamentDetailResponse(savedTournament);
    }

    @Transactional(readOnly = true)
    public List<ClawgicTournamentResponses.TournamentSummary> listUpcomingTournaments() {
        List<ClawgicTournament> upcomingTournaments =
                clawgicTournamentRepository.findByStatusAndStartTimeAfterOrderByStartTimeAsc(
                        ClawgicTournamentStatus.SCHEDULED,
                        OffsetDateTime.now()
                );
        return clawgicResponseMapper.toTournamentSummaryResponses(upcomingTournaments);
    }

    private OffsetDateTime resolveEntryCloseTime(OffsetDateTime startTime, OffsetDateTime requestedEntryCloseTime) {
        OffsetDateTime entryCloseTime = requestedEntryCloseTime;
        if (entryCloseTime == null) {
            int defaultEntryWindowMinutes = clawgicRuntimeProperties.getTournament().getDefaultEntryWindowMinutes();
            if (defaultEntryWindowMinutes < 0) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "clawgic.tournament.default-entry-window-minutes must be non-negative"
                );
            }
            entryCloseTime = startTime.minusMinutes(defaultEntryWindowMinutes);
        }

        if (entryCloseTime.isAfter(startTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "entryCloseTime must be on or before startTime"
            );
        }
        return entryCloseTime;
    }
}
