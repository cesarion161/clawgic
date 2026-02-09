"""ELO rating system implementation.

Formulas:
- Expected score: E_X = 1 / (1 + 10^((R_Y - R_X) / 400))
- Rating update: R_X_new = R_X + K(1 - E_X)
  where K is weighted by total stake
"""

import math
from typing import Tuple
from .models import Post, Curator


class ELOSystem:
    """Manages ELO rating calculations and updates."""

    def __init__(self, base_k: float = 32.0):
        """Initialize ELO system.

        Args:
            base_k: Base K-factor for rating updates
        """
        self.base_k = base_k

    def calculate_expected_score(self, rating_x: float, rating_y: float) -> float:
        """Calculate expected score for X against Y.

        E_X = 1 / (1 + 10^((R_Y - R_X) / 400))

        Args:
            rating_x: Rating of entity X
            rating_y: Rating of entity Y

        Returns:
            Expected score between 0 and 1
        """
        exponent = (rating_y - rating_x) / 400.0
        return 1.0 / (1.0 + math.pow(10, exponent))

    def calculate_k_factor(self, total_stake: float) -> float:
        """Calculate K-factor weighted by total stake.

        Args:
            total_stake: Total stake involved in the comparison

        Returns:
            Weighted K-factor
        """
        # K increases with total stake to make high-stakes votes more impactful
        stake_multiplier = 1.0 + (total_stake / 10000.0)  # Scale factor
        return self.base_k * stake_multiplier

    def update_rating(
        self,
        rating: float,
        expected_score: float,
        actual_score: float,
        k_factor: float
    ) -> float:
        """Update rating based on actual vs expected performance.

        R_new = R + K(S - E)
        where S is actual score (1 for win, 0 for loss)

        Args:
            rating: Current rating
            expected_score: Expected score (from calculate_expected_score)
            actual_score: Actual score (1.0 for win, 0.0 for loss, 0.5 for tie)
            k_factor: K-factor for this update

        Returns:
            Updated rating
        """
        return rating + k_factor * (actual_score - expected_score)

    def update_post_ratings(
        self,
        winner: Post,
        loser: Post,
        total_stake: float
    ) -> Tuple[float, float]:
        """Update ratings for two posts after a comparison.

        Args:
            winner: Post that won the comparison
            loser: Post that lost the comparison
            total_stake: Total stake of curators who voted

        Returns:
            Tuple of (new_winner_rating, new_loser_rating)
        """
        k = self.calculate_k_factor(total_stake)

        # Calculate expected scores
        expected_winner = self.calculate_expected_score(
            winner.elo_rating,
            loser.elo_rating
        )
        expected_loser = self.calculate_expected_score(
            loser.elo_rating,
            winner.elo_rating
        )

        # Update ratings (winner gets 1.0, loser gets 0.0)
        new_winner_rating = self.update_rating(
            winner.elo_rating,
            expected_winner,
            1.0,
            k
        )
        new_loser_rating = self.update_rating(
            loser.elo_rating,
            expected_loser,
            0.0,
            k
        )

        # Update post statistics
        winner.total_comparisons += 1
        winner.wins += 1
        loser.total_comparisons += 1
        loser.losses += 1

        return new_winner_rating, new_loser_rating

    def update_curator_rating(
        self,
        curator: Curator,
        voted_correctly: bool,
        total_stake: float
    ) -> float:
        """Update curator rating based on vote accuracy.

        Args:
            curator: Curator whose rating to update
            voted_correctly: Whether curator voted with majority
            total_stake: Total stake involved

        Returns:
            Updated curator rating
        """
        k = self.calculate_k_factor(total_stake)

        # Simple expected score (assumes average opponent)
        expected = 0.5

        # Actual score: 1.0 if correct, 0.0 if incorrect
        actual = 1.0 if voted_correctly else 0.0

        new_rating = self.update_rating(
            curator.elo_rating,
            expected,
            actual,
            k
        )

        # Update curator statistics
        curator.total_votes += 1
        if voted_correctly:
            curator.correct_votes += 1

        return new_rating
