package com.clawgic.service;

import com.clawgic.model.Pair;
import com.clawgic.model.RoundStatus;
import com.clawgic.repository.PairRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Pageable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PairSelectionServiceTest {

    private static final String WALLET = "test-wallet";

    @Mock
    private PairRepository pairRepository;

    @InjectMocks
    private PairSelectionService pairSelectionService;

    @Test
    void findNextPairForCurator_queriesCommitPhaseOnly() {
        Pair pair = new Pair();
        when(pairRepository.findNextPairsForCurator(
                eq(WALLET),
                eq(1),
                eq(List.of(RoundStatus.COMMIT)),
                any(Pageable.class)))
                .thenReturn(List.of(pair));

        Optional<Pair> result = pairSelectionService.findNextPairForCurator(WALLET, 1);

        assertSame(pair, result.orElseThrow());
        verify(pairRepository).findNextPairsForCurator(
                eq(WALLET),
                eq(1),
                eq(List.of(RoundStatus.COMMIT)),
                any(Pageable.class));
    }

    @Test
    void findNextPairForCurator_returnsFirstWhenMultipleCandidatesExist() {
        Pair first = new Pair();
        first.setId(1);
        Pair second = new Pair();
        second.setId(2);

        when(pairRepository.findNextPairsForCurator(
                eq(WALLET),
                eq(1),
                eq(List.of(RoundStatus.COMMIT)),
                any(Pageable.class)))
                .thenReturn(List.of(first, second));

        Optional<Pair> result = pairSelectionService.findNextPairForCurator(WALLET, 1);

        assertEquals(1, result.orElseThrow().getId());
    }

    @ParameterizedTest
    @EnumSource(RoundStatus.class)
    void isEligibleCurationPhase_allowsOnlyCommit(RoundStatus status) {
        assertEquals(status == RoundStatus.COMMIT, pairSelectionService.isEligibleCurationPhase(status));
    }
}
