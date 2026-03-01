import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ClawgicAgentsPage from './page'

const mockFetch = vi.fn()

type MockResponseInit = {
  ok: boolean
  status: number
  statusText: string
  jsonBody?: unknown
  textBody?: string
}

const agentsFixture = [
  {
    agentId: '00000000-0000-0000-0000-000000000a01',
    walletAddress: '0x1111111111111111111111111111111111111111',
    name: 'Logic Falcon',
    avatarUrl: null,
    providerType: 'OPENAI',
    providerKeyRef: 'gpt-4o',
    persona: 'A sharp analytical debater focused on formal logic.',
    createdAt: '2026-02-28T10:00:00Z',
    updatedAt: '2026-02-28T10:00:00Z',
  },
  {
    agentId: '00000000-0000-0000-0000-000000000a02',
    walletAddress: '0x2222222222222222222222222222222222222222',
    name: 'Counter Fox',
    avatarUrl: null,
    providerType: 'ANTHROPIC',
    providerKeyRef: null,
    persona: null,
    createdAt: '2026-02-28T11:00:00Z',
    updatedAt: '2026-02-28T11:00:00Z',
  },
]

function mockResponse(init: MockResponseInit) {
  return {
    ok: init.ok,
    status: init.status,
    statusText: init.statusText,
    json: () => Promise.resolve(init.jsonBody),
    text: () => Promise.resolve(init.textBody || ''),
  }
}

describe('ClawgicAgentsPage', () => {
  beforeEach(() => {
    mockFetch.mockReset()
    globalThis.fetch = mockFetch
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders loading then agent list on successful fetch', async () => {
    mockFetch.mockResolvedValueOnce(
      mockResponse({
        ok: true,
        status: 200,
        statusText: 'OK',
        jsonBody: agentsFixture,
      })
    )

    render(<ClawgicAgentsPage />)

    expect(screen.getByText('Loading agents...')).toBeInTheDocument()
    expect(await screen.findByText('Logic Falcon')).toBeInTheDocument()
    expect(screen.getByText('Counter Fox')).toBeInTheDocument()
    expect(screen.getByText('Provider: OPENAI')).toBeInTheDocument()
    expect(screen.getByText('Provider: ANTHROPIC')).toBeInTheDocument()
    expect(screen.getByText('Model: gpt-4o')).toBeInTheDocument()
    expect(screen.getByText('A sharp analytical debater focused on formal logic.')).toBeInTheDocument()
  })

  it('renders empty state when no agents exist', async () => {
    mockFetch.mockResolvedValueOnce(
      mockResponse({
        ok: true,
        status: 200,
        statusText: 'OK',
        jsonBody: [],
      })
    )

    render(<ClawgicAgentsPage />)

    expect(await screen.findByText(/No agents found/)).toBeInTheDocument()
  })

  it('renders error state when fetch fails', async () => {
    mockFetch.mockRejectedValueOnce(new TypeError('fetch failed'))

    render(<ClawgicAgentsPage />)

    expect(await screen.findByText('Failed to load agents.')).toBeInTheDocument()
    expect(screen.getByText('Network error: fetch failed')).toBeInTheDocument()
  })

  it('renders Clawgic badge and navigation links', async () => {
    mockFetch.mockResolvedValueOnce(
      mockResponse({
        ok: true,
        status: 200,
        statusText: 'OK',
        jsonBody: agentsFixture,
      })
    )

    render(<ClawgicAgentsPage />)

    expect(await screen.findByText('Logic Falcon')).toBeInTheDocument()
    expect(screen.getByText('Clawgic')).toHaveClass('clawgic-badge')
    expect(screen.getByRole('link', { name: 'Tournament Lobby' })).toHaveAttribute('href', '/clawgic/tournaments')
    expect(screen.getByRole('link', { name: 'Leaderboard' })).toHaveAttribute('href', '/clawgic/leaderboard')
  })

  it('renders provider badge per agent card', async () => {
    mockFetch.mockResolvedValueOnce(
      mockResponse({
        ok: true,
        status: 200,
        statusText: 'OK',
        jsonBody: [agentsFixture[0]],
      })
    )

    render(<ClawgicAgentsPage />)

    expect(await screen.findByText('Logic Falcon')).toBeInTheDocument()
    const badges = screen.getAllByText('OPENAI')
    expect(badges.length).toBeGreaterThanOrEqual(1)
    expect(badges.some((el) => el.classList.contains('clawgic-badge'))).toBe(true)
  })

  it('does not render Model field when providerKeyRef is null', async () => {
    mockFetch.mockResolvedValueOnce(
      mockResponse({
        ok: true,
        status: 200,
        statusText: 'OK',
        jsonBody: [agentsFixture[1]],
      })
    )

    render(<ClawgicAgentsPage />)

    expect(await screen.findByText('Counter Fox')).toBeInTheDocument()
    expect(screen.queryByText(/Model:/)).not.toBeInTheDocument()
  })

  it('truncates long wallet addresses', async () => {
    mockFetch.mockResolvedValueOnce(
      mockResponse({
        ok: true,
        status: 200,
        statusText: 'OK',
        jsonBody: [agentsFixture[0]],
      })
    )

    render(<ClawgicAgentsPage />)

    expect(await screen.findByText('Logic Falcon')).toBeInTheDocument()
    // Full address is 42 chars, should be truncated to 14 + '...'
    expect(screen.getByText('0x111111111111...')).toBeInTheDocument()
  })
})
