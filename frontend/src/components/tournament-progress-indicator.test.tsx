import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import TournamentProgressIndicator from './tournament-progress-indicator'

const resolveAgentName = (id: string | null) => {
  if (!id) return 'TBD'
  const names: Record<string, string> = {
    'a1': 'AlphaBot',
    'a2': 'BetaBot',
    'a3': 'GammaBot',
    'a4': 'DeltaBot',
  }
  return names[id] ?? id
}

function makeBracket(overrides?: {
  sf1Status?: string
  sf1Winner?: string | null
  sf2Status?: string
  sf2Winner?: string | null
  finalStatus?: string
  finalWinner?: string | null
}) {
  return [
    {
      matchId: 'sf1',
      status: overrides?.sf1Status ?? 'SCHEDULED',
      bracketRound: 1,
      bracketPosition: 1,
      winnerAgentId: overrides?.sf1Winner ?? null,
    },
    {
      matchId: 'sf2',
      status: overrides?.sf2Status ?? 'SCHEDULED',
      bracketRound: 1,
      bracketPosition: 2,
      winnerAgentId: overrides?.sf2Winner ?? null,
    },
    {
      matchId: 'final',
      status: overrides?.finalStatus ?? 'SCHEDULED',
      bracketRound: 2,
      bracketPosition: 1,
      winnerAgentId: overrides?.finalWinner ?? null,
    },
  ]
}

describe('TournamentProgressIndicator', () => {
  it('renders nothing when bracket is empty', () => {
    const { container } = render(
      <TournamentProgressIndicator bracket={[]} activeMatchId={null} resolveAgentName={resolveAgentName} />
    )
    expect(container.innerHTML).toBe('')
  })

  it('renders three stages: Semifinal 1, Semifinal 2, Final', () => {
    render(
      <TournamentProgressIndicator
        bracket={makeBracket()}
        activeMatchId={null}
        resolveAgentName={resolveAgentName}
      />
    )
    expect(screen.getByTestId('tournament-progress')).toBeInTheDocument()
    expect(screen.getByText('Semifinal 1')).toBeInTheDocument()
    expect(screen.getByText('Semifinal 2')).toBeInTheDocument()
    expect(screen.getByText('Final')).toBeInTheDocument()
  })

  it('shows all stages as pending when no matches are active or completed', () => {
    render(
      <TournamentProgressIndicator
        bracket={makeBracket()}
        activeMatchId={null}
        resolveAgentName={resolveAgentName}
      />
    )
    const stages = screen.getAllByTestId(/^stage-/)
    expect(stages).toHaveLength(3)
    // All stage dots should be slate (pending)
    for (const stage of stages) {
      const dot = stage.querySelector('.bg-slate-300')
      expect(dot).toBeInTheDocument()
    }
  })

  it('marks semifinal 1 as active when its match is active', () => {
    render(
      <TournamentProgressIndicator
        bracket={makeBracket({ sf1Status: 'IN_PROGRESS' })}
        activeMatchId="sf1"
        resolveAgentName={resolveAgentName}
      />
    )
    const stage0 = screen.getByTestId('stage-0')
    const dot = stage0.querySelector('.bg-blue-500')
    expect(dot).toBeInTheDocument()
  })

  it('marks semifinal 1 as completed when match is COMPLETED', () => {
    render(
      <TournamentProgressIndicator
        bracket={makeBracket({ sf1Status: 'COMPLETED', sf1Winner: 'a1' })}
        activeMatchId={null}
        resolveAgentName={resolveAgentName}
      />
    )
    const stage0 = screen.getByTestId('stage-0')
    const dot = stage0.querySelector('.bg-emerald-500')
    expect(dot).toBeInTheDocument()
  })

  it('marks semifinal as forfeited when match is FORFEITED', () => {
    render(
      <TournamentProgressIndicator
        bracket={makeBracket({ sf1Status: 'FORFEITED', sf1Winner: 'a1' })}
        activeMatchId={null}
        resolveAgentName={resolveAgentName}
      />
    )
    const stage0 = screen.getByTestId('stage-0')
    const dot = stage0.querySelector('.bg-red-400')
    expect(dot).toBeInTheDocument()
  })

  it('shows correct mixed states: sf1 completed, sf2 active, final pending', () => {
    render(
      <TournamentProgressIndicator
        bracket={makeBracket({ sf1Status: 'COMPLETED', sf1Winner: 'a1', sf2Status: 'IN_PROGRESS' })}
        activeMatchId="sf2"
        resolveAgentName={resolveAgentName}
      />
    )
    // SF1 completed (emerald)
    const stage0 = screen.getByTestId('stage-0')
    expect(stage0.querySelector('.bg-emerald-500')).toBeInTheDocument()
    // SF2 active (blue)
    const stage1 = screen.getByTestId('stage-1')
    expect(stage1.querySelector('.bg-blue-500')).toBeInTheDocument()
    // Final pending (slate)
    const stage2 = screen.getByTestId('stage-2')
    expect(stage2.querySelector('.bg-slate-300')).toBeInTheDocument()
  })

  it('shows all stages as completed when tournament is done', () => {
    render(
      <TournamentProgressIndicator
        bracket={makeBracket({
          sf1Status: 'COMPLETED',
          sf1Winner: 'a1',
          sf2Status: 'COMPLETED',
          sf2Winner: 'a2',
          finalStatus: 'COMPLETED',
          finalWinner: 'a1',
        })}
        activeMatchId={null}
        resolveAgentName={resolveAgentName}
      />
    )
    const stages = screen.getAllByTestId(/^stage-/)
    for (const stage of stages) {
      expect(stage.querySelector('.bg-emerald-500')).toBeInTheDocument()
    }
  })

  it('marks PENDING_JUDGE match as active stage', () => {
    render(
      <TournamentProgressIndicator
        bracket={makeBracket({ sf1Status: 'PENDING_JUDGE' })}
        activeMatchId={null}
        resolveAgentName={resolveAgentName}
      />
    )
    const stage0 = screen.getByTestId('stage-0')
    // PENDING_JUDGE counts as anyInProgress
    const dot = stage0.querySelector('.bg-blue-500')
    expect(dot).toBeInTheDocument()
  })
})
