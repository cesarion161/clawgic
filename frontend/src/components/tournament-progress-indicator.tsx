'use client'

type BracketMatchState = {
  matchId: string
  status: string
  bracketRound: number | null
  bracketPosition: number | null
  winnerAgentId: string | null
}

export type TournamentProgressIndicatorProps = {
  bracket: BracketMatchState[]
  activeMatchId: string | null
  resolveAgentName: (agentId: string | null) => string
}

type StageInfo = {
  label: string
  status: 'pending' | 'active' | 'completed' | 'forfeited'
  winnerName: string | null
}

function resolveStage(
  matches: BracketMatchState[],
  activeMatchId: string | null,
  resolveAgentName: (id: string | null) => string,
): StageInfo {
  if (matches.length === 0) return { label: '', status: 'pending', winnerName: null }

  const hasActive = matches.some((m) => m.matchId === activeMatchId)
  const allTerminal = matches.every((m) => m.status === 'COMPLETED' || m.status === 'FORFEITED')
  const anyInProgress = matches.some((m) => m.status === 'IN_PROGRESS' || m.status === 'PENDING_JUDGE')

  if (allTerminal) {
    const allForfeited = matches.every((m) => m.status === 'FORFEITED')
    const winner = matches.length === 1 ? matches[0].winnerAgentId : null
    return {
      label: '',
      status: allForfeited ? 'forfeited' : 'completed',
      winnerName: winner ? resolveAgentName(winner) : null,
    }
  }

  if (hasActive || anyInProgress) {
    return { label: '', status: 'active', winnerName: null }
  }

  return { label: '', status: 'pending', winnerName: null }
}

export default function TournamentProgressIndicator({
  bracket,
  activeMatchId,
  resolveAgentName,
}: TournamentProgressIndicatorProps) {
  if (bracket.length === 0) return null

  const sf1 = bracket.filter((m) => m.bracketRound === 1 && m.bracketPosition === 1)
  const sf2 = bracket.filter((m) => m.bracketRound === 1 && m.bracketPosition === 2)
  const finalMatches = bracket.filter((m) => m.bracketRound === 2)

  const stages: (StageInfo & { label: string })[] = [
    { ...resolveStage(sf1, activeMatchId, resolveAgentName), label: 'Semifinal 1' },
    { ...resolveStage(sf2, activeMatchId, resolveAgentName), label: 'Semifinal 2' },
    { ...resolveStage(finalMatches, activeMatchId, resolveAgentName), label: 'Final' },
  ]

  return (
    <div className="flex items-center gap-1" data-testid="tournament-progress">
      {stages.map((stage, i) => {
        const dotColor =
          stage.status === 'completed'
            ? 'bg-emerald-500'
            : stage.status === 'active'
              ? 'bg-blue-500 animate-pulse'
              : stage.status === 'forfeited'
                ? 'bg-red-400'
                : 'bg-slate-300'

        const textColor =
          stage.status === 'completed'
            ? 'text-emerald-800 font-semibold'
            : stage.status === 'active'
              ? 'text-blue-800 font-semibold'
              : stage.status === 'forfeited'
                ? 'text-red-700'
                : 'text-muted-foreground'

        return (
          <div key={stage.label} className="flex items-center gap-1" data-testid={`stage-${i}`}>
            {i > 0 && (
              <svg className="h-3 w-3 text-slate-300 mx-0.5" fill="none" viewBox="0 0 24 24" strokeWidth={2.5} stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
              </svg>
            )}
            <span className={`inline-block h-2 w-2 rounded-full ${dotColor}`} />
            <span className={`text-xs ${textColor}`}>{stage.label}</span>
          </div>
        )
      })}
    </div>
  )
}
