package com.moltrank.service;

import com.moltrank.model.Market;
import com.moltrank.model.Round;
import com.moltrank.repository.CuratorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuratorSupplyServiceTest {

    @Mock
    private CuratorRepository curatorRepository;

    private CuratorSupplyService curatorSupplyService;

    @BeforeEach
    void setUp() {
        curatorSupplyService = new CuratorSupplyService(curatorRepository);
    }

    @Test
    void computeForRound_calculatesRequiredCuratorsAndSupplyRatio() {
        ReflectionTestUtils.setField(curatorSupplyService, "targetRevealsPerPair", 3);
        ReflectionTestUtils.setField(curatorSupplyService, "expectedRevealsPerCurator", 6);
        when(curatorRepository.countByMarketId(1)).thenReturn(5L);

        CuratorSupplyService.CuratorSupplySnapshot snapshot = curatorSupplyService.computeForRound(
                buildRound(1, 12)
        );

        assertEquals(12, snapshot.generatedPairs());
        assertEquals(3, snapshot.targetRevealsPerPair());
        assertEquals(6, snapshot.expectedRevealsPerCurator());
        assertEquals(6, snapshot.requiredCurators());
        assertEquals(5, snapshot.activeCurators());
        assertEquals(new BigDecimal("0.8333"), snapshot.supplyRatio());
    }

    @Test
    void computeForRound_handlesZeroPairsAndZeroActiveCurators() {
        ReflectionTestUtils.setField(curatorSupplyService, "targetRevealsPerPair", 3);
        ReflectionTestUtils.setField(curatorSupplyService, "expectedRevealsPerCurator", 6);
        when(curatorRepository.countByMarketId(1)).thenReturn(0L);

        CuratorSupplyService.CuratorSupplySnapshot snapshot = curatorSupplyService.computeForRound(
                buildRound(1, 0)
        );

        assertEquals(0, snapshot.requiredCurators());
        assertEquals(0, snapshot.activeCurators());
        assertEquals(new BigDecimal("1.0000"), snapshot.supplyRatio());
    }

    @Test
    void computeForRound_handlesZeroActiveCuratorsWithDemand() {
        ReflectionTestUtils.setField(curatorSupplyService, "targetRevealsPerPair", 3);
        ReflectionTestUtils.setField(curatorSupplyService, "expectedRevealsPerCurator", 4);
        when(curatorRepository.countByMarketId(1)).thenReturn(0L);

        CuratorSupplyService.CuratorSupplySnapshot snapshot = curatorSupplyService.computeForRound(
                buildRound(1, 4)
        );

        assertEquals(3, snapshot.requiredCurators());
        assertEquals(0, snapshot.activeCurators());
        assertEquals(new BigDecimal("0.0000"), snapshot.supplyRatio());
    }

    @Test
    void computeForRound_handlesTinyMarketAtLeastOneRequiredCurator() {
        ReflectionTestUtils.setField(curatorSupplyService, "targetRevealsPerPair", 2);
        ReflectionTestUtils.setField(curatorSupplyService, "expectedRevealsPerCurator", 10);
        when(curatorRepository.countByMarketId(1)).thenReturn(1L);

        CuratorSupplyService.CuratorSupplySnapshot snapshot = curatorSupplyService.computeForRound(
                buildRound(1, 1)
        );

        assertEquals(1, snapshot.requiredCurators());
        assertEquals(1, snapshot.activeCurators());
        assertEquals(new BigDecimal("1.0000"), snapshot.supplyRatio());
    }

    @Test
    void computeForRound_normalizesInvalidConfigToOne() {
        ReflectionTestUtils.setField(curatorSupplyService, "targetRevealsPerPair", 0);
        ReflectionTestUtils.setField(curatorSupplyService, "expectedRevealsPerCurator", -2);
        when(curatorRepository.countByMarketId(1)).thenReturn(3L);

        CuratorSupplyService.CuratorSupplySnapshot snapshot = curatorSupplyService.computeForRound(
                buildRound(1, 2)
        );

        assertEquals(1, snapshot.targetRevealsPerPair());
        assertEquals(1, snapshot.expectedRevealsPerCurator());
        assertEquals(2, snapshot.requiredCurators());
    }

    @Test
    void computeForRound_throwsWhenRoundHasNoMarketContext() {
        Round round = new Round();
        round.setId(10);
        round.setPairs(5);

        assertThrows(IllegalArgumentException.class, () -> curatorSupplyService.computeForRound(round));
    }

    private Round buildRound(Integer marketId, Integer pairs) {
        Market market = new Market();
        market.setId(marketId);
        market.setName("General");
        market.setSubmoltId("general");

        Round round = new Round();
        round.setId(1);
        round.setMarket(market);
        round.setPairs(pairs);
        return round;
    }
}
