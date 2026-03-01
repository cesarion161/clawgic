'use client'

export type MatchStatusBadgeProps = {
  status: string
  winnerName?: string | null
  forfeitReason?: string | null
  size?: 'sm' | 'md'
}

export default function MatchStatusBadge({
  status,
  winnerName,
  forfeitReason,
  size = 'md',
}: MatchStatusBadgeProps) {
  const textSize = size === 'sm' ? 'text-[10px]' : 'text-xs'

  switch (status) {
    case 'SCHEDULED':
      return (
        <span
          className={`clawgic-badge border-slate-400/40 bg-slate-50 text-slate-700 ${textSize}`}
          data-testid="match-status-badge"
          data-status="SCHEDULED"
        >
          Waiting
        </span>
      )

    case 'IN_PROGRESS':
      return (
        <span
          className={`clawgic-badge border-blue-400/40 bg-blue-50 text-blue-800 ${textSize}`}
          data-testid="match-status-badge"
          data-status="IN_PROGRESS"
        >
          <span className="mr-1.5 inline-block h-2 w-2 rounded-full bg-blue-500 animate-pulse" />
          Battling
        </span>
      )

    case 'PENDING_JUDGE':
      return (
        <span
          className={`clawgic-badge border-amber-400/40 bg-amber-50 text-amber-800 ${textSize}`}
          data-testid="match-status-badge"
          data-status="PENDING_JUDGE"
        >
          <span className="mr-1.5 inline-block h-3 w-3 rounded-full border-2 border-amber-500 border-t-transparent animate-spin" />
          Awaiting Judge
        </span>
      )

    case 'COMPLETED':
      return (
        <span
          className={`clawgic-badge border-emerald-400/40 bg-emerald-50 text-emerald-800 ${textSize}`}
          data-testid="match-status-badge"
          data-status="COMPLETED"
        >
          Judged{winnerName ? ` \u2014 ${winnerName}` : ''}
        </span>
      )

    case 'FORFEITED':
      return (
        <span
          className={`clawgic-badge border-red-400/40 bg-red-50 text-red-800 ${textSize}`}
          data-testid="match-status-badge"
          data-status="FORFEITED"
          title={forfeitReason ?? undefined}
        >
          Forfeited
        </span>
      )

    default:
      return (
        <span
          className={`clawgic-badge border-slate-400/40 bg-slate-50 text-slate-700 ${textSize}`}
          data-testid="match-status-badge"
          data-status={status}
        >
          {status}
        </span>
      )
  }
}
