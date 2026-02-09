"""Core data models for the simulation."""

from dataclasses import dataclass, field
from typing import Dict, List, Optional
from enum import Enum


class Vote(Enum):
    """Curator vote options."""
    LEFT = "left"
    RIGHT = "right"
    NO_REVEAL = "no_reveal"


@dataclass
class GlobalPool:
    """Tracks the global reward pool and its distribution.

    Balance = subscriptions + slashing + minority losses
    Alpha-split calculation:
        base = pool × alpha / total_pairs
        premium = pool × (1 - alpha) × market_share
    """

    balance: float = 0.0
    alpha: float = 0.30  # Default alpha parameter
    total_subscriptions: float = 0.0
    total_slashing: float = 0.0
    total_minority_losses: float = 0.0

    def add_subscription(self, amount: float) -> None:
        """Add subscription revenue to the pool."""
        self.balance += amount
        self.total_subscriptions += amount

    def add_slashing(self, amount: float) -> None:
        """Add slashed funds to the pool."""
        self.balance += amount
        self.total_slashing += amount

    def add_minority_loss(self, amount: float) -> None:
        """Add minority losses to the pool."""
        self.balance += amount
        self.total_minority_losses += amount

    def calculate_base_reward(self, total_pairs: int) -> float:
        """Calculate base reward per pair: pool × alpha / total_pairs."""
        if total_pairs == 0:
            return 0.0
        return self.balance * self.alpha / total_pairs

    def calculate_premium_reward(self, market_share: float) -> float:
        """Calculate premium reward: pool × (1 - alpha) × market_share."""
        return self.balance * (1 - self.alpha) * market_share

    def withdraw(self, amount: float) -> None:
        """Withdraw from the pool for rewards distribution."""
        if amount > self.balance:
            raise ValueError(f"Insufficient pool balance: {self.balance} < {amount}")
        self.balance -= amount


@dataclass
class CuratorScore:
    """Tracks curator performance metrics.

    score = w1×CalibrationRate + w2×AlignmentStability + w3×AuditPassRate - w4×FraudFlags
    """

    curator_id: str
    calibration_rate: float = 0.0  # Accuracy on Golden Set
    alignment_stability: float = 0.0  # Consistency with majority
    audit_pass_rate: float = 0.0  # Consistency on Audit Pairs
    fraud_flags: int = 0  # Behavioral anomalies detected

    # Weights for score calculation
    w1: float = 0.40  # Golden Set weight
    w2: float = 0.25  # Alignment weight
    w3: float = 0.20  # Audit weight
    w4: float = 0.15  # Fraud penalty weight

    def calculate_score(self) -> float:
        """Calculate the curator's overall score."""
        return (
            self.w1 * self.calibration_rate +
            self.w2 * self.alignment_stability +
            self.w3 * self.audit_pass_rate -
            self.w4 * self.fraud_flags
        )

    def is_eligible_for_full_rewards(self) -> bool:
        """Check if curator qualifies for full rewards (>60%)."""
        return self.calculate_score() > 0.60

    def is_eligible_for_reduced_rewards(self) -> bool:
        """Check if curator qualifies for reduced rewards (40-60%)."""
        score = self.calculate_score()
        return 0.40 <= score <= 0.60

    def should_be_slashed(self) -> bool:
        """Check if curator should be slashed and suspended (<40%)."""
        return self.calculate_score() < 0.40


@dataclass
class Curator:
    """Represents a curator in the system."""

    curator_id: str
    elo_rating: float = 1500.0  # Default ELO rating
    stake: float = 0.0
    total_votes: int = 0
    correct_votes: int = 0
    score: Optional[CuratorScore] = None

    def __post_init__(self):
        if self.score is None:
            self.score = CuratorScore(curator_id=self.curator_id)


@dataclass
class Post:
    """Represents a post to be ranked."""

    post_id: str
    content: str
    elo_rating: float = 1500.0  # Default ELO rating
    total_comparisons: int = 0
    wins: int = 0
    losses: int = 0


@dataclass
class Pair:
    """Represents a pair of posts for comparison."""

    pair_id: str
    post_left: Post
    post_right: Post
    is_golden_set: bool = False
    is_audit_pair: bool = False
    golden_correct_answer: Optional[Vote] = None  # For Golden Set pairs
    votes: Dict[str, Vote] = field(default_factory=dict)  # curator_id -> vote

    def add_vote(self, curator_id: str, vote: Vote) -> None:
        """Record a curator's vote."""
        self.votes[curator_id] = vote

    def get_majority_vote(self) -> Optional[Vote]:
        """Get the majority vote (excluding NO_REVEAL)."""
        vote_counts = {Vote.LEFT: 0, Vote.RIGHT: 0}
        for vote in self.votes.values():
            if vote in vote_counts:
                vote_counts[vote] += 1

        if vote_counts[Vote.LEFT] > vote_counts[Vote.RIGHT]:
            return Vote.LEFT
        elif vote_counts[Vote.RIGHT] > vote_counts[Vote.LEFT]:
            return Vote.RIGHT
        return None  # Tie

    def get_minority_voters(self) -> List[str]:
        """Get list of curator IDs who voted with the minority."""
        majority = self.get_majority_vote()
        if majority is None:
            return []

        return [
            curator_id for curator_id, vote in self.votes.items()
            if vote != Vote.NO_REVEAL and vote != majority
        ]

    def get_no_reveal_voters(self) -> List[str]:
        """Get list of curator IDs who didn't reveal their vote."""
        return [
            curator_id for curator_id, vote in self.votes.items()
            if vote == Vote.NO_REVEAL
        ]


@dataclass
class Round:
    """Represents a single round of comparisons."""

    round_id: int
    pairs: List[Pair] = field(default_factory=list)
    curators: List[Curator] = field(default_factory=list)
    pool: Optional[GlobalPool] = None

    def __post_init__(self):
        if self.pool is None:
            self.pool = GlobalPool()
