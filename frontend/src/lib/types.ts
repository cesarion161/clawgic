// Core domain types

export interface Post {
  id: string
  content: string
  author: string
  timestamp: number
  eloRating: number
  category?: string
}

export interface User {
  id: string
  publicKey: string
  username?: string
  curatorRating: number
}

export interface Comparison {
  id: string
  postA: Post
  postB: Post
  winner?: string
  curatorId: string
  timestamp: number
}

export interface PoolHealth {
  totalPosts: number
  activeCurators: number
  avgEloRating: number
  timestamp: number
}

// Curator types for dashboard and leaderboard
export interface CuratorStats {
  wallet: string
  earned: number
  lost: number
  net: number
  curatorScore: number
  calibrationRate: number
  auditPassRate: number
  alignmentStability: number
  fraudFlags: number
}

export interface CuratorEvaluation {
  id: string
  pair: string
  choice: string
  outcome: 'win' | 'loss'
  amount: number
  timestamp: number
}

export interface LeaderboardEntry {
  rank: number
  wallet: string
  xHandle?: string
  curatorScore: number
  calibrationRate: number
  auditPassRate: number
  totalEarned: number
  isCurrentUser?: boolean
}
