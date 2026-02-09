"""Example simulation demonstrating the core engine functionality."""

from src import (
    SimulationEngine,
    SimulationConfig,
    Curator,
    Post,
    Vote,
)


def run_example_simulation():
    """Run a simple example simulation."""

    # Create simulation with config
    config = SimulationConfig(
        random_seed=42,  # For reproducibility
        demand_gate_k=1.5,
        golden_set_percentage=0.10,
        audit_pair_percentage=0.05,
    )

    engine = SimulationEngine(config)

    # Add initial pool balance (from subscriptions)
    engine.pool.add_subscription(10000.0)

    # Create curators
    curators = [
        Curator(curator_id="curator_1", stake=1000.0),
        Curator(curator_id="curator_2", stake=1500.0),
        Curator(curator_id="curator_3", stake=800.0),
        Curator(curator_id="curator_4", stake=1200.0),
        Curator(curator_id="curator_5", stake=900.0),
    ]

    for curator in curators:
        engine.add_curator(curator)

    # Create posts
    posts = [
        Post(post_id=f"post_{i}", content=f"Post content {i}")
        for i in range(1, 21)  # 20 posts
    ]

    for post in posts:
        engine.add_post(post)

    # Create Golden Set pairs (with known correct answers)
    golden_set = [
        (posts[0], posts[1], Vote.LEFT),  # post_1 should win
        (posts[2], posts[3], Vote.RIGHT),  # post_4 should win
    ]

    print("=" * 70)
    print("MOLTBOOK SIMULATION - EXAMPLE RUN")
    print("=" * 70)
    print(f"\nInitial Configuration:")
    print(f"  Curators: {len(curators)}")
    print(f"  Posts: {len(posts)}")
    print(f"  Pool Balance: ${engine.pool.balance:,.2f}")
    print(f"  Alpha: {engine.pool.alpha}")
    print()

    # Run multiple rounds
    num_rounds = 3
    num_subscribers = 100

    for round_num in range(num_rounds):
        print(f"\n{'='*70}")
        print(f"ROUND {round_num + 1}")
        print(f"{'='*70}")

        # Run round
        round, results = engine.run_round(
            num_subscribers=num_subscribers,
            golden_set_pairs=golden_set if round_num == 0 else []
        )

        # Display round results
        print(f"\nPairs generated: {len(round.pairs)}")
        print(f"  - Golden Set: {sum(1 for p in round.pairs if p.is_golden_set)}")
        print(f"  - Audit Pairs: {sum(1 for p in round.pairs if p.is_audit_pair)}")
        print(f"  - Regular Pairs: {sum(1 for p in round.pairs if not p.is_golden_set and not p.is_audit_pair)}")

        print(f"\nCurator Results:")
        print(f"  {'Curator':<12} {'Rewards':>10} {'Slashed':>10} {'Score':>8} {'ELO':>8} {'Status':<12}")
        print(f"  {'-'*12} {'-'*10} {'-'*10} {'-'*8} {'-'*8} {'-'*12}")

        for curator in curators:
            result = results.get(curator.curator_id, {})
            rewards = result.get('rewards', 0.0)
            slashed = result.get('slashed', 0.0)
            score = result.get('score', 0.0)
            suspended = result.get('suspended', False)
            status = "SUSPENDED" if suspended else "Active"

            print(f"  {curator.curator_id:<12} ${rewards:>9.2f} ${slashed:>9.2f} {score:>8.2%} {curator.elo_rating:>8.0f} {status:<12}")

        print(f"\nTop 5 Posts by ELO:")
        sorted_posts = sorted(posts, key=lambda p: p.elo_rating, reverse=True)[:5]
        print(f"  {'Post':<12} {'ELO':>8} {'Wins':>6} {'Losses':>6} {'Comparisons':>12}")
        print(f"  {'-'*12} {'-'*8} {'-'*6} {'-'*6} {'-'*12}")
        for post in sorted_posts:
            print(f"  {post.post_id:<12} {post.elo_rating:>8.0f} {post.wins:>6} {post.losses:>6} {post.total_comparisons:>12}")

    # Final statistics
    print(f"\n{'='*70}")
    print("FINAL STATISTICS")
    print(f"{'='*70}")

    stats = engine.get_statistics()
    print(f"\nPool Status:")
    print(f"  Final Balance: ${stats['pool_balance']:,.2f}")
    print(f"  Total Subscriptions: ${stats['pool_stats']['subscriptions']:,.2f}")
    print(f"  Total Slashing: ${stats['pool_stats']['slashing']:,.2f}")
    print(f"  Total Minority Losses: ${stats['pool_stats']['minority_losses']:,.2f}")

    print(f"\nOverall:")
    print(f"  Total Rounds: {stats['total_rounds']}")
    print(f"  Total Curators: {stats['total_curators']}")
    print(f"  Total Posts: {stats['total_posts']}")

    print(f"\n{'='*70}")
    print("SIMULATION COMPLETE")
    print(f"{'='*70}\n")


if __name__ == "__main__":
    run_example_simulation()
