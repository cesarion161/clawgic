"""Blended slashing system implementation.

Components:
- Golden Set accuracy (40%)
- Audit consistency (25%)
- Long-window consensus (20%)
- Behavioral anomalies (15%)

Thresholds:
- Above 60%: full rewards
- 40-60%: 50% reduced rewards
- Below 40%: 10% slash + suspension
"""

from typing import Dict, List, Tuple
from .models import Curator, CuratorScore, Pair, Vote


class SlashingSystem:
    """Manages curator slashing based on performance metrics."""

    # Component weights
    GOLDEN_SET_WEIGHT = 0.40
    AUDIT_WEIGHT = 0.25
    CONSENSUS_WEIGHT = 0.20
    BEHAVIORAL_WEIGHT = 0.15

    # Reward thresholds
    FULL_REWARDS_THRESHOLD = 0.60
    REDUCED_REWARDS_THRESHOLD = 0.40
    SLASH_PERCENTAGE = 0.10  # 10% slash for poor performance

    def __init__(self):
        """Initialize slashing system."""
        self.curator_scores: Dict[str, CuratorScore] = {}

    def get_or_create_score(self, curator_id: str) -> CuratorScore:
        """Get existing score or create new one for curator."""
        if curator_id not in self.curator_scores:
            self.curator_scores[curator_id] = CuratorScore(curator_id=curator_id)
        return self.curator_scores[curator_id]

    def update_golden_set_accuracy(
        self,
        curator_id: str,
        correct_count: int,
        total_count: int
    ) -> None:
        """Update Golden Set accuracy metric.

        Args:
            curator_id: ID of curator
            correct_count: Number of correct Golden Set votes
            total_count: Total Golden Set votes
        """
        score = self.get_or_create_score(curator_id)
        if total_count > 0:
            score.calibration_rate = correct_count / total_count
        else:
            score.calibration_rate = 0.0

    def update_audit_consistency(
        self,
        curator_id: str,
        consistent_count: int,
        total_audit_pairs: int
    ) -> None:
        """Update Audit Pair consistency metric.

        Args:
            curator_id: ID of curator
            consistent_count: Number of consistent audit votes
            total_audit_pairs: Total audit pairs
        """
        score = self.get_or_create_score(curator_id)
        if total_audit_pairs > 0:
            score.audit_pass_rate = consistent_count / total_audit_pairs
        else:
            score.audit_pass_rate = 0.0

    def update_alignment_stability(
        self,
        curator_id: str,
        majority_votes: int,
        total_votes: int
    ) -> None:
        """Update long-window consensus alignment metric.

        Args:
            curator_id: ID of curator
            majority_votes: Number of votes aligned with majority
            total_votes: Total votes cast
        """
        score = self.get_or_create_score(curator_id)
        if total_votes > 0:
            score.alignment_stability = majority_votes / total_votes
        else:
            score.alignment_stability = 0.0

    def flag_behavioral_anomaly(self, curator_id: str) -> None:
        """Flag a behavioral anomaly for a curator.

        Examples: rapid voting, consistent minority voting, bot-like patterns.
        """
        score = self.get_or_create_score(curator_id)
        score.fraud_flags += 1

    def calculate_curator_score(self, curator_id: str) -> float:
        """Calculate overall curator score.

        Returns:
            Score between 0 and 1 (can be negative with fraud flags)
        """
        score = self.get_or_create_score(curator_id)
        return score.calculate_score()

    def determine_reward_multiplier(self, curator_id: str) -> float:
        """Determine reward multiplier based on curator score.

        Returns:
            1.0 for full rewards (>60%)
            0.5 for reduced rewards (40-60%)
            0.0 for slashed curators (<40%)
        """
        score = self.calculate_curator_score(curator_id)

        if score >= self.FULL_REWARDS_THRESHOLD:
            return 1.0
        elif score >= self.REDUCED_REWARDS_THRESHOLD:
            return 0.5
        else:
            return 0.0

    def should_suspend(self, curator_id: str) -> bool:
        """Check if curator should be suspended.

        Returns:
            True if score is below 40%
        """
        score = self.calculate_curator_score(curator_id)
        return score < self.REDUCED_REWARDS_THRESHOLD

    def calculate_slash_amount(
        self,
        curator_id: str,
        curator_stake: float
    ) -> float:
        """Calculate amount to slash from curator's stake.

        Args:
            curator_id: ID of curator
            curator_stake: Curator's current stake

        Returns:
            Amount to slash (0 if not slashable)
        """
        if self.should_suspend(curator_id):
            return curator_stake * self.SLASH_PERCENTAGE
        return 0.0

    def process_round_metrics(
        self,
        pairs: List[Pair],
        curators: List[Curator]
    ) -> Dict[str, Tuple[float, bool, float]]:
        """Process all pairs in a round and update curator metrics.

        Args:
            pairs: List of pairs voted on in the round
            curators: List of curators who voted

        Returns:
            Dict mapping curator_id to (score, should_suspend, slash_amount)
        """
        # Track metrics per curator
        curator_metrics: Dict[str, Dict] = {}
        for curator in curators:
            curator_metrics[curator.curator_id] = {
                'golden_correct': 0,
                'golden_total': 0,
                'audit_consistent': 0,
                'audit_total': 0,
                'majority_votes': 0,
                'total_votes': 0,
            }

        # Process each pair
        for pair in pairs:
            majority_vote = pair.get_majority_vote()

            for curator in curators:
                vote = pair.votes.get(curator.curator_id)
                if vote is None or vote == Vote.NO_REVEAL:
                    continue

                metrics = curator_metrics[curator.curator_id]

                # Golden Set accuracy
                if pair.is_golden_set and pair.golden_correct_answer:
                    metrics['golden_total'] += 1
                    if vote == pair.golden_correct_answer:
                        metrics['golden_correct'] += 1

                # Audit consistency (simplified: did they vote the same as before?)
                if pair.is_audit_pair:
                    metrics['audit_total'] += 1
                    # In real implementation, check against historical vote
                    # For now, just check if they voted with majority
                    if vote == majority_vote:
                        metrics['audit_consistent'] += 1

                # Alignment with majority
                metrics['total_votes'] += 1
                if vote == majority_vote:
                    metrics['majority_votes'] += 1

        # Update scores and calculate results
        results = {}
        for curator in curators:
            metrics = curator_metrics[curator.curator_id]

            # Update all metrics
            self.update_golden_set_accuracy(
                curator.curator_id,
                metrics['golden_correct'],
                metrics['golden_total']
            )
            self.update_audit_consistency(
                curator.curator_id,
                metrics['audit_consistent'],
                metrics['audit_total']
            )
            self.update_alignment_stability(
                curator.curator_id,
                metrics['majority_votes'],
                metrics['total_votes']
            )

            # Calculate final results
            score = self.calculate_curator_score(curator.curator_id)
            should_suspend = self.should_suspend(curator.curator_id)
            slash_amount = self.calculate_slash_amount(
                curator.curator_id,
                curator.stake
            )

            results[curator.curator_id] = (score, should_suspend, slash_amount)

        return results
