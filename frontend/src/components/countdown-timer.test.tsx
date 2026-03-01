import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, act } from '@testing-library/react'
import CountdownTimer, { computeRemainingMs, formatCountdown } from './countdown-timer'

describe('formatCountdown', () => {
  it('formats zero as 00:00', () => {
    expect(formatCountdown(0)).toBe('00:00')
  })

  it('formats seconds correctly', () => {
    expect(formatCountdown(45_000)).toBe('00:45')
  })

  it('formats minutes and seconds', () => {
    expect(formatCountdown(125_000)).toBe('02:05')
  })

  it('formats hours', () => {
    expect(formatCountdown(3_661_000)).toBe('01:01:01')
  })

  it('formats days', () => {
    expect(formatCountdown(90_061_000)).toBe('1d 01:01:01')
  })

  it('returns 00:00 for negative values', () => {
    expect(formatCountdown(-1000)).toBe('00:00')
  })
})

describe('computeRemainingMs', () => {
  it('returns positive ms when target is in the future', () => {
    const target = new Date(Date.now() + 60_000).toISOString()
    const result = computeRemainingMs(target)
    expect(result).toBeGreaterThan(0)
    expect(result).toBeLessThanOrEqual(60_000)
  })

  it('returns 0 when target is in the past', () => {
    const target = new Date(Date.now() - 60_000).toISOString()
    expect(computeRemainingMs(target)).toBe(0)
  })

  it('returns 0 for invalid date', () => {
    expect(computeRemainingMs('invalid')).toBe(0)
  })

  it('adjusts for server time skew', () => {
    const now = Date.now()
    const target = new Date(now + 120_000).toISOString()
    // Server clock is 60s behind local clock. From the server's perspective,
    // its "now" is now-60s, so target (now+120s) is 180s away in server time.
    const serverTime = new Date(now - 60_000).toISOString()
    const result = computeRemainingMs(target, serverTime)
    expect(result).toBeGreaterThan(170_000)
    expect(result).toBeLessThanOrEqual(190_000)
  })
})

describe('CountdownTimer component', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders countdown for future target', () => {
    const target = new Date(Date.now() + 125_000).toISOString()
    render(<CountdownTimer targetTime={target} />)
    const timer = screen.getByTestId('countdown-timer')
    expect(timer.textContent).toMatch(/\d{2}:\d{2}/)
    expect(timer.textContent).not.toBe('Entry Closed')
  })

  it('renders Entry Closed for past target', () => {
    const target = new Date(Date.now() - 60_000).toISOString()
    render(<CountdownTimer targetTime={target} />)
    expect(screen.getByTestId('countdown-timer')).toHaveTextContent('Entry Closed')
  })

  it('calls onExpired when countdown reaches zero', () => {
    const onExpired = vi.fn()
    const target = new Date(Date.now() + 2_000).toISOString()
    render(<CountdownTimer targetTime={target} onExpired={onExpired} />)

    expect(onExpired).not.toHaveBeenCalled()

    act(() => {
      vi.advanceTimersByTime(3_000)
    })

    expect(onExpired).toHaveBeenCalled()
  })

  it('shows Entry Closed text after expiry', () => {
    const target = new Date(Date.now() + 2_000).toISOString()
    render(<CountdownTimer targetTime={target} />)

    expect(screen.getByTestId('countdown-timer')).not.toHaveTextContent('Entry Closed')

    act(() => {
      vi.advanceTimersByTime(3_000)
    })

    expect(screen.getByTestId('countdown-timer')).toHaveTextContent('Entry Closed')
  })

  it('ticks every second', () => {
    const target = new Date(Date.now() + 10_000).toISOString()
    render(<CountdownTimer targetTime={target} />)

    const initialText = screen.getByTestId('countdown-timer').textContent

    act(() => {
      vi.advanceTimersByTime(1_000)
    })

    const afterOneSecond = screen.getByTestId('countdown-timer').textContent
    // The countdown should have changed after 1 second
    expect(afterOneSecond).not.toBe(initialText)
  })

  it('applies custom className', () => {
    const target = new Date(Date.now() + 10_000).toISOString()
    render(<CountdownTimer targetTime={target} className="text-lg" />)
    expect(screen.getByTestId('countdown-timer').className).toContain('text-lg')
  })

  it('has aria-label with countdown value', () => {
    const target = new Date(Date.now() + 125_000).toISOString()
    render(<CountdownTimer targetTime={target} />)
    const timer = screen.getByTestId('countdown-timer')
    expect(timer.getAttribute('aria-label')).toMatch(/Countdown:/)
  })

  it('has aria-label Entry Closed when expired', () => {
    const target = new Date(Date.now() - 60_000).toISOString()
    render(<CountdownTimer targetTime={target} />)
    expect(screen.getByTestId('countdown-timer').getAttribute('aria-label')).toBe('Entry Closed')
  })
})
