import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import MatchStatusBadge from './match-status-badge'

describe('MatchStatusBadge', () => {
  it('renders SCHEDULED state as gray "Waiting" badge', () => {
    render(<MatchStatusBadge status="SCHEDULED" />)
    const badge = screen.getByTestId('match-status-badge')
    expect(badge).toHaveTextContent('Waiting')
    expect(badge).toHaveAttribute('data-status', 'SCHEDULED')
    expect(badge.className).toContain('bg-slate-50')
  })

  it('renders IN_PROGRESS state as blue "Battling" badge with pulsing indicator', () => {
    render(<MatchStatusBadge status="IN_PROGRESS" />)
    const badge = screen.getByTestId('match-status-badge')
    expect(badge).toHaveTextContent('Battling')
    expect(badge).toHaveAttribute('data-status', 'IN_PROGRESS')
    expect(badge.className).toContain('bg-blue-50')
    // Should contain a pulsing dot child
    const dot = badge.querySelector('.animate-pulse')
    expect(dot).toBeInTheDocument()
  })

  it('renders PENDING_JUDGE state as amber "Awaiting Judge" badge with spinner', () => {
    render(<MatchStatusBadge status="PENDING_JUDGE" />)
    const badge = screen.getByTestId('match-status-badge')
    expect(badge).toHaveTextContent('Awaiting Judge')
    expect(badge).toHaveAttribute('data-status', 'PENDING_JUDGE')
    expect(badge.className).toContain('bg-amber-50')
    // Should contain a spinning indicator
    const spinner = badge.querySelector('.animate-spin')
    expect(spinner).toBeInTheDocument()
  })

  it('renders COMPLETED state as green "Judged" badge', () => {
    render(<MatchStatusBadge status="COMPLETED" />)
    const badge = screen.getByTestId('match-status-badge')
    expect(badge).toHaveTextContent('Judged')
    expect(badge).toHaveAttribute('data-status', 'COMPLETED')
    expect(badge.className).toContain('bg-emerald-50')
  })

  it('renders COMPLETED state with winner name when provided', () => {
    render(<MatchStatusBadge status="COMPLETED" winnerName="AlphaBot" />)
    const badge = screen.getByTestId('match-status-badge')
    expect(badge).toHaveTextContent('Judged')
    expect(badge).toHaveTextContent('AlphaBot')
  })

  it('renders FORFEITED state as red "Forfeited" badge', () => {
    render(<MatchStatusBadge status="FORFEITED" />)
    const badge = screen.getByTestId('match-status-badge')
    expect(badge).toHaveTextContent('Forfeited')
    expect(badge).toHaveAttribute('data-status', 'FORFEITED')
    expect(badge.className).toContain('bg-red-50')
  })

  it('renders FORFEITED state with tooltip containing forfeit reason', () => {
    render(<MatchStatusBadge status="FORFEITED" forfeitReason="PROVIDER_TIMEOUT: agent xyz timed out" />)
    const badge = screen.getByTestId('match-status-badge')
    expect(badge).toHaveAttribute('title', 'PROVIDER_TIMEOUT: agent xyz timed out')
  })

  it('renders unknown status as-is with default styling', () => {
    render(<MatchStatusBadge status="CANCELLED" />)
    const badge = screen.getByTestId('match-status-badge')
    expect(badge).toHaveTextContent('CANCELLED')
    expect(badge).toHaveAttribute('data-status', 'CANCELLED')
  })

  it('renders small size with text-[10px]', () => {
    render(<MatchStatusBadge status="SCHEDULED" size="sm" />)
    const badge = screen.getByTestId('match-status-badge')
    expect(badge.className).toContain('text-[10px]')
  })

  it('renders medium size with text-xs by default', () => {
    render(<MatchStatusBadge status="SCHEDULED" />)
    const badge = screen.getByTestId('match-status-badge')
    expect(badge.className).toContain('text-xs')
  })

  it('does not show winner name in COMPLETED badge when winnerName is null', () => {
    render(<MatchStatusBadge status="COMPLETED" winnerName={null} />)
    const badge = screen.getByTestId('match-status-badge')
    expect(badge.textContent).toBe('Judged')
  })

  it('does not set title on FORFEITED badge when forfeitReason is null', () => {
    render(<MatchStatusBadge status="FORFEITED" forfeitReason={null} />)
    const badge = screen.getByTestId('match-status-badge')
    expect(badge).not.toHaveAttribute('title')
  })
})
