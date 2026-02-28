package com.clawgic.service;

/**
 * Generic Elo math helpers shared across legacy MoltRank and Clawgic flows.
 */
public final class EloRatingCalculator {

    private EloRatingCalculator() {
    }

    public static RatingPair calculateRatings(
            int playerOneRating,
            int playerTwoRating,
            double playerOneScore,
            double playerTwoScore,
            double kFactor
    ) {
        if (kFactor <= 0.0d || Double.isNaN(kFactor) || Double.isInfinite(kFactor)) {
            throw new IllegalArgumentException("kFactor must be a positive finite value");
        }

        double expectedPlayerOne = expectedScore(playerOneRating, playerTwoRating);
        double expectedPlayerTwo = expectedScore(playerTwoRating, playerOneRating);

        int updatedPlayerOne = (int) Math.round(playerOneRating + kFactor * (playerOneScore - expectedPlayerOne));
        int updatedPlayerTwo = (int) Math.round(playerTwoRating + kFactor * (playerTwoScore - expectedPlayerTwo));

        return new RatingPair(updatedPlayerOne, updatedPlayerTwo);
    }

    public static double expectedScore(int rating, int opponentRating) {
        return 1.0d / (1.0d + Math.pow(10.0d, (opponentRating - rating) / 400.0d));
    }

    public record RatingPair(int playerOneRating, int playerTwoRating) {
    }
}
