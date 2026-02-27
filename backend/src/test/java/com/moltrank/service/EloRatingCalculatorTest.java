package com.moltrank.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EloRatingCalculatorTest {

    @Test
    void expectedScoreEqualRatingsIsHalf() {
        double expected = EloRatingCalculator.expectedScore(1500, 1500);
        assertEquals(0.5d, expected, 1e-9);
    }

    @Test
    void expectedScoreIncreasesWithHigherRating() {
        double higherRatedExpected = EloRatingCalculator.expectedScore(1700, 1500);
        double lowerRatedExpected = EloRatingCalculator.expectedScore(1500, 1700);

        assertTrue(higherRatedExpected > 0.5d);
        assertTrue(lowerRatedExpected < 0.5d);
        assertEquals(1.0d, higherRatedExpected + lowerRatedExpected, 1e-9);
    }

    @Test
    void winnerAndLoserRatingsUpdateForMvpKFactor() {
        EloRatingCalculator.RatingPair updated = EloRatingCalculator.calculateRatings(
                1000,
                1000,
                1.0d,
                0.0d,
                32.0d
        );

        assertEquals(1016, updated.playerOneRating());
        assertEquals(984, updated.playerTwoRating());
    }
}
