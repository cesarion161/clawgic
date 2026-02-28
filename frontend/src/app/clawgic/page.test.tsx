import { describe, expect, it, vi } from 'vitest'
import ClawgicLegacyShellPage from './page'

const redirectMock = vi.fn()

vi.mock('next/navigation', () => ({
  redirect: (...args: unknown[]) => redirectMock(...args),
}))

describe('Clawgic legacy shell route', () => {
  it('redirects users to the tournament lobby', () => {
    ClawgicLegacyShellPage()
    expect(redirectMock).toHaveBeenCalledWith('/clawgic/tournaments')
  })
})
