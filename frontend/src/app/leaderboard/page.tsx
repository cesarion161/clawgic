'use client'

import { useState, useEffect } from 'react'
import { apiClient } from '@/lib/api-client'
import { LeaderboardEntry } from '@/lib/types'

export default function LeaderboardPage() {
  const [entries, setEntries] = useState<LeaderboardEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [currentPage, setCurrentPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)

  const pageSize = 50
  const currentWallet = '4Nd1mYQzvgV8Vr3Z3nYb7pD6T8K9jF2eqWxY1S3Qh5Ro' // Mock current user wallet

  useEffect(() => {
    async function fetchLeaderboard() {
      try {
        setLoading(true)

        // TODO: Replace with real API call when backend is ready
        // For now, using mock data
        const mockEntries: LeaderboardEntry[] = [
          {
            rank: 1,
            wallet: 'Abc1...xyz9',
            xHandle: '@topCurator',
            curatorScore: 95.8,
            calibrationRate: 0.98,
            auditPassRate: 0.99,
            totalEarned: 45230,
          },
          {
            rank: 2,
            wallet: 'Def2...uvw8',
            xHandle: '@eliteJudge',
            curatorScore: 93.2,
            calibrationRate: 0.96,
            auditPassRate: 0.97,
            totalEarned: 42150,
          },
          {
            rank: 3,
            wallet: 'Ghi3...rst7',
            curatorScore: 91.5,
            calibrationRate: 0.94,
            auditPassRate: 0.96,
            totalEarned: 39840,
          },
          {
            rank: 4,
            wallet: 'Jkl4...opq6',
            xHandle: '@contentKing',
            curatorScore: 89.7,
            calibrationRate: 0.92,
            auditPassRate: 0.94,
            totalEarned: 37520,
          },
          {
            rank: 5,
            wallet: 'Mno5...lmn5',
            curatorScore: 88.3,
            calibrationRate: 0.91,
            auditPassRate: 0.93,
            totalEarned: 35410,
          },
          {
            rank: 6,
            wallet: currentWallet,
            xHandle: '@you',
            curatorScore: 87.5,
            calibrationRate: 0.92,
            auditPassRate: 0.95,
            totalEarned: 15420,
            isCurrentUser: true,
          },
          {
            rank: 7,
            wallet: 'Pqr6...ijk4',
            curatorScore: 86.1,
            calibrationRate: 0.89,
            auditPassRate: 0.91,
            totalEarned: 32190,
          },
          {
            rank: 8,
            wallet: 'Stu7...fgh3',
            xHandle: '@curator99',
            curatorScore: 84.9,
            calibrationRate: 0.88,
            auditPassRate: 0.90,
            totalEarned: 30850,
          },
          {
            rank: 9,
            wallet: 'Vwx8...cde2',
            curatorScore: 83.2,
            calibrationRate: 0.87,
            auditPassRate: 0.89,
            totalEarned: 29340,
          },
          {
            rank: 10,
            wallet: 'Yza9...abc1',
            xHandle: '@expertCurator',
            curatorScore: 82.4,
            calibrationRate: 0.86,
            auditPassRate: 0.88,
            totalEarned: 28120,
          },
        ]

        setEntries(mockEntries)
        setTotalPages(Math.ceil(mockEntries.length / pageSize))
        setLoading(false)

        // Uncomment when backend is ready:
        // const data = await apiClient.getLeaderboard(1, currentPage, pageSize)
        // setEntries(data.entries)
        // setTotalPages(data.totalPages)
        // setLoading(false)
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load leaderboard')
        setLoading(false)
      }
    }

    fetchLeaderboard()
  }, [currentPage])

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Loading leaderboard...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="text-center">
          <p className="text-red-500 mb-2">Error loading leaderboard</p>
          <p className="text-sm text-muted-foreground">{error}</p>
        </div>
      </div>
    )
  }

  const handlePageChange = (page: number) => {
    if (page >= 1 && page <= totalPages) {
      setCurrentPage(page)
    }
  }

  const getRankBadgeColor = (rank: number) => {
    if (rank === 1) return 'bg-yellow-500/20 text-yellow-500 border-yellow-500/50'
    if (rank === 2) return 'bg-gray-400/20 text-gray-300 border-gray-400/50'
    if (rank === 3) return 'bg-orange-600/20 text-orange-400 border-orange-600/50'
    return 'bg-secondary text-muted-foreground border-border'
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="space-y-2">
        <h1 className="text-4xl font-bold">Curator Leaderboard</h1>
        <p className="text-muted-foreground">
          Top curators ranked by CuratorScore, not raw earnings
        </p>
      </div>

      {/* Leaderboard Table */}
      <div className="bg-card rounded-lg border border-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-secondary">
              <tr>
                <th className="text-left py-4 px-6 text-sm font-semibold">Rank</th>
                <th className="text-left py-4 px-6 text-sm font-semibold">Curator</th>
                <th className="text-right py-4 px-6 text-sm font-semibold">CuratorScore</th>
                <th className="text-right py-4 px-6 text-sm font-semibold">
                  Calibration Rate
                </th>
                <th className="text-right py-4 px-6 text-sm font-semibold">Audit Pass Rate</th>
                <th className="text-right py-4 px-6 text-sm font-semibold">Total Earned</th>
              </tr>
            </thead>
            <tbody>
              {entries.map((entry) => (
                <tr
                  key={entry.wallet}
                  className={`border-b border-border transition-colors ${
                    entry.isCurrentUser
                      ? 'bg-primary/10 hover:bg-primary/15'
                      : 'hover:bg-secondary/50'
                  }`}
                >
                  {/* Rank */}
                  <td className="py-4 px-6">
                    <div
                      className={`inline-flex items-center justify-center w-10 h-10 rounded-full border font-bold ${getRankBadgeColor(
                        entry.rank
                      )}`}
                    >
                      {entry.rank}
                    </div>
                  </td>

                  {/* Curator */}
                  <td className="py-4 px-6">
                    <div>
                      <div className="font-medium">
                        {entry.xHandle || entry.wallet}
                        {entry.isCurrentUser && (
                          <span className="ml-2 text-xs bg-primary/20 text-primary px-2 py-0.5 rounded-full">
                            You
                          </span>
                        )}
                      </div>
                      {entry.xHandle && (
                        <div className="text-xs text-muted-foreground font-mono mt-0.5">
                          {entry.wallet}
                        </div>
                      )}
                    </div>
                  </td>

                  {/* CuratorScore */}
                  <td className="py-4 px-6 text-right">
                    <span className="text-lg font-bold">{entry.curatorScore.toFixed(1)}</span>
                  </td>

                  {/* Calibration Rate */}
                  <td className="py-4 px-6 text-right">
                    <div className="flex items-center justify-end gap-2">
                      <span className="text-sm font-medium">
                        {(entry.calibrationRate * 100).toFixed(1)}%
                      </span>
                      <div className="w-16 bg-secondary rounded-full h-2">
                        <div
                          className="bg-blue-500 h-2 rounded-full"
                          style={{ width: `${entry.calibrationRate * 100}%` }}
                        ></div>
                      </div>
                    </div>
                  </td>

                  {/* Audit Pass Rate */}
                  <td className="py-4 px-6 text-right">
                    <div className="flex items-center justify-end gap-2">
                      <span className="text-sm font-medium">
                        {(entry.auditPassRate * 100).toFixed(1)}%
                      </span>
                      <div className="w-16 bg-secondary rounded-full h-2">
                        <div
                          className="bg-green-500 h-2 rounded-full"
                          style={{ width: `${entry.auditPassRate * 100}%` }}
                        ></div>
                      </div>
                    </div>
                  </td>

                  {/* Total Earned */}
                  <td className="py-4 px-6 text-right">
                    <span className="text-sm font-medium text-muted-foreground">
                      {entry.totalEarned.toLocaleString()}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between px-6 py-4 bg-secondary border-t border-border">
            <div className="text-sm text-muted-foreground">
              Page {currentPage} of {totalPages}
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 1}
                className="px-4 py-2 text-sm font-medium rounded-md bg-card border border-border hover:bg-secondary disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                Previous
              </button>
              <button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage === totalPages}
                className="px-4 py-2 text-sm font-medium rounded-md bg-card border border-border hover:bg-secondary disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Footer Note */}
      <div className="bg-card rounded-lg border border-border p-4">
        <div className="flex items-start gap-3">
          <div className="text-blue-500 mt-0.5">ℹ️</div>
          <div className="text-sm space-y-1">
            <p className="font-medium">Rankings by CuratorScore</p>
            <p className="text-muted-foreground">
              Leaderboard is sorted by CuratorScore, which combines calibration accuracy, audit
              performance, and alignment stability. Total earnings are shown as a secondary metric
              but do not affect ranking.
            </p>
          </div>
        </div>
      </div>

      {/* Footer Reference */}
      <div className="text-center text-sm text-muted-foreground border-t border-border pt-6">
        <p>Reference: PRD Sections 8.4, 8.7, 11</p>
      </div>
    </div>
  )
}
