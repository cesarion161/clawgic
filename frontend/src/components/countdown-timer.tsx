'use client'

import { useEffect, useState } from 'react'

type CountdownTimerProps = {
  targetTime: string
  serverTime?: string
  onExpired?: () => void
  className?: string
}

function computeRemainingMs(targetTime: string, serverTime?: string): number {
  const targetMs = new Date(targetTime).getTime()
  if (Number.isNaN(targetMs)) return 0

  let nowMs = Date.now()
  if (serverTime) {
    const serverMs = new Date(serverTime).getTime()
    if (!Number.isNaN(serverMs)) {
      const skew = Date.now() - serverMs
      nowMs = Date.now() - skew
    }
  }

  return Math.max(0, targetMs - nowMs)
}

function formatCountdown(remainingMs: number): string {
  if (remainingMs <= 0) return '00:00'

  const totalSeconds = Math.floor(remainingMs / 1000)
  const days = Math.floor(totalSeconds / 86400)
  const hours = Math.floor((totalSeconds % 86400) / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  const pad = (n: number) => n.toString().padStart(2, '0')

  if (days > 0) {
    return `${days}d ${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
  }
  if (hours > 0) {
    return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
  }
  return `${pad(minutes)}:${pad(seconds)}`
}

export default function CountdownTimer({
  targetTime,
  serverTime,
  onExpired,
  className,
}: CountdownTimerProps) {
  const [remainingMs, setRemainingMs] = useState(() =>
    computeRemainingMs(targetTime, serverTime)
  )

  useEffect(() => {
    // Compute clock offset once per prop change so ticking advances correctly.
    // offset = local clock minus server clock; positive means our clock is ahead.
    let clockOffsetMs = 0
    if (serverTime) {
      const serverMs = new Date(serverTime).getTime()
      if (!Number.isNaN(serverMs)) {
        clockOffsetMs = Date.now() - serverMs
      }
    }

    function computeRemaining(): number {
      const targetMs = new Date(targetTime).getTime()
      if (Number.isNaN(targetMs)) return 0
      const adjustedNow = Date.now() - clockOffsetMs
      return Math.max(0, targetMs - adjustedNow)
    }

    setRemainingMs(computeRemaining())

    const interval = setInterval(() => {
      const ms = computeRemaining()
      setRemainingMs(ms)
      if (ms <= 0) {
        clearInterval(interval)
        onExpired?.()
      }
    }, 1000)

    return () => clearInterval(interval)
  }, [targetTime, serverTime, onExpired])

  const expired = remainingMs <= 0

  return (
    <span
      className={`font-mono tabular-nums ${expired ? 'text-red-600' : ''} ${className || ''}`}
      data-testid="countdown-timer"
      aria-label={expired ? 'Entry Closed' : `Countdown: ${formatCountdown(remainingMs)}`}
    >
      {expired ? 'Entry Closed' : formatCountdown(remainingMs)}
    </span>
  )
}

export { computeRemainingMs, formatCountdown }
