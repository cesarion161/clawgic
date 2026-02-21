package com.moltrank.service;

import com.moltrank.model.Pair;
import com.moltrank.model.RoundStatus;
import com.moltrank.repository.PairRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Selects curator-facing pairs based on round phase eligibility.
 */
@Service
@RequiredArgsConstructor
public class PairSelectionService {

    private static final List<RoundStatus> ELIGIBLE_CURATION_PHASES = List.of(RoundStatus.COMMIT);

    private final PairRepository pairRepository;

    public Optional<Pair> findNextPairForCurator(String wallet, Integer marketId) {
        List<Pair> matches = pairRepository.findNextPairsForCurator(
                wallet,
                marketId,
                ELIGIBLE_CURATION_PHASES,
                PageRequest.of(0, 1)
        );
        return matches.stream().findFirst();
    }

    boolean isEligibleCurationPhase(RoundStatus status) {
        return ELIGIBLE_CURATION_PHASES.contains(status);
    }
}
