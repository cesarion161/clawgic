package com.moltrank.service;

import com.moltrank.model.GlobalPool;
import com.moltrank.model.Market;
import com.moltrank.model.Round;
import com.moltrank.model.RoundStatus;
import com.moltrank.repository.GlobalPoolRepository;
import com.moltrank.repository.MarketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PoolServiceTest {

    @Mock
    private GlobalPoolRepository globalPoolRepository;

    @Mock
    private MarketRepository marketRepository;

    @InjectMocks
    private PoolService poolService;

    private GlobalPool pool;
    private Market market;
    private Round round;

    @BeforeEach
    void setUp() {
        pool = new GlobalPool();
        pool.setId(1);
        pool.setBalance(10_000_000_000L); // 10 SOL
        pool.setAlpha(new BigDecimal("0.30"));

        market = new Market();
        market.setId(1);
        market.setName("tech");
        market.setSubmoltId("tech");
        market.setSubscriptionRevenue(5_000_000_000L);
        market.setSubscribers(10);

        round = new Round();
        round.setId(1);
        round.setMarket(market);
        round.setStatus(RoundStatus.SETTLING);
        round.setPairs(20);
    }

    @Test
    void calculateRewards_baseAndPremiumCorrect() {
        when(globalPoolRepository.findById(1)).thenReturn(Optional.of(pool));
        when(marketRepository.findAll()).thenReturn(List.of(market));

        long[] rewards = poolService.calculateRewards(round, 20);

        long basePerPair = rewards[0];
        long premiumPerPair = rewards[1];

        // Base: 10B * 0.30 / 20 = 150,000,000
        assertEquals(150_000_000L, basePerPair);

        // Premium: 10B * 0.70 * (5B/5B) / 20 = 350,000,000
        assertEquals(350_000_000L, premiumPerPair);
    }

    @Test
    void calculateRewards_multipleMarkets_weightsByRevenue() {
        Market market2 = new Market();
        market2.setId(2);
        market2.setName("science");
        market2.setSubmoltId("science");
        market2.setSubscriptionRevenue(15_000_000_000L);
        market2.setSubscribers(20);

        when(globalPoolRepository.findById(1)).thenReturn(Optional.of(pool));
        when(marketRepository.findAll()).thenReturn(List.of(market, market2));

        long[] rewards = poolService.calculateRewards(round, 20);

        long premiumPerPair = rewards[1];

        // Market has 5B out of total 20B revenue
        // Premium: 10B * 0.70 * (5B/20B) / 20 = 87,500,000
        assertEquals(87_500_000L, premiumPerPair);
    }

    @Test
    void calculateRewards_zeroPairs_returnsZero() {
        long[] rewards = poolService.calculateRewards(round, 0);
        assertEquals(0L, rewards[0]);
        assertEquals(0L, rewards[1]);
    }

    @Test
    void calculateRewards_zeroRevenue_premiumIsZero() {
        market.setSubscriptionRevenue(0L);
        when(globalPoolRepository.findById(1)).thenReturn(Optional.of(pool));
        when(marketRepository.findAll()).thenReturn(List.of(market));

        long[] rewards = poolService.calculateRewards(round, 20);

        assertTrue(rewards[0] > 0, "Base should still be positive");
        assertEquals(0L, rewards[1], "Premium should be zero with no revenue");
    }

    @Test
    void addToPool_increasesBalance() {
        when(globalPoolRepository.findById(1)).thenReturn(Optional.of(pool));
        when(globalPoolRepository.save(any(GlobalPool.class))).thenAnswer(inv -> inv.getArgument(0));

        poolService.addToPool(1_000_000_000L, "test");

        assertEquals(11_000_000_000L, pool.getBalance());
        verify(globalPoolRepository).save(pool);
    }

    @Test
    void deductFromPool_decreasesBalance() {
        when(globalPoolRepository.findById(1)).thenReturn(Optional.of(pool));
        when(globalPoolRepository.save(any(GlobalPool.class))).thenAnswer(inv -> inv.getArgument(0));

        poolService.deductFromPool(3_000_000_000L, "test");

        assertEquals(7_000_000_000L, pool.getBalance());
    }

    @Test
    void deductFromPool_insufficientBalance_throws() {
        when(globalPoolRepository.findById(1)).thenReturn(Optional.of(pool));

        assertThrows(IllegalStateException.class,
                () -> poolService.deductFromPool(20_000_000_000L, "test"));
    }

    @Test
    void getPoolBalance_returnsCurrentBalance() {
        when(globalPoolRepository.findById(1)).thenReturn(Optional.of(pool));

        assertEquals(10_000_000_000L, poolService.getPoolBalance());
    }

    @Test
    void updateAlpha_validRange_succeeds() {
        when(globalPoolRepository.findById(1)).thenReturn(Optional.of(pool));
        when(globalPoolRepository.save(any(GlobalPool.class))).thenAnswer(inv -> inv.getArgument(0));

        poolService.updateAlpha(new BigDecimal("0.50"));

        assertEquals(new BigDecimal("0.50"), pool.getAlpha());
    }

    @Test
    void updateAlpha_outOfRange_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> poolService.updateAlpha(new BigDecimal("1.5")));
        assertThrows(IllegalArgumentException.class,
                () -> poolService.updateAlpha(new BigDecimal("-0.1")));
    }
}
