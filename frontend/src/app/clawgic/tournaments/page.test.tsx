import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ClawgicTournamentLobbyPage from './page'

const mockFetch = vi.fn()

type MockResponseInit = {
  ok: boolean
  status: number
  statusText: string
  jsonBody?: unknown
  textBody?: string
}

const tournamentsFixture = [
  {
    tournamentId: '00000000-0000-0000-0000-000000000901',
    topic: 'Debate on deterministic mocks',
    status: 'SCHEDULED',
    bracketSize: 4,
    maxEntries: 4,
    startTime: '2026-03-01T14:00:00Z',
    entryCloseTime: '2026-03-01T13:00:00Z',
    baseEntryFeeUsdc: '5.000000',
  },
]

const agentsFixture = [
  {
    agentId: '00000000-0000-0000-0000-000000000911',
    walletAddress: '0x1111111111111111111111111111111111111111',
    name: 'Logic Falcon',
    providerType: 'OPENAI',
  },
  {
    agentId: '00000000-0000-0000-0000-000000000912',
    walletAddress: '0x2222222222222222222222222222222222222222',
    name: 'Counter Fox',
    providerType: 'ANTHROPIC',
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

describe('ClawgicTournamentLobbyPage', () => {
  beforeEach(() => {
    globalThis.fetch = mockFetch
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders loading then tournament details on successful data fetch', async () => {
    mockFetch
      .mockResolvedValueOnce(
        mockResponse({
          ok: true,
          status: 200,
          statusText: 'OK',
          jsonBody: tournamentsFixture,
        })
      )
      .mockResolvedValueOnce(
        mockResponse({
          ok: true,
          status: 200,
          statusText: 'OK',
          jsonBody: agentsFixture,
        })
      )

    render(<ClawgicTournamentLobbyPage />)

    expect(screen.getByText('Loading tournament lobby...')).toBeInTheDocument()
    expect(await screen.findByText('Debate on deterministic mocks')).toBeInTheDocument()
    expect(screen.getByText('Status: SCHEDULED')).toBeInTheDocument()
    expect(screen.getByText('Entry fee: 5.00 USDC')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Enter Tournament' })).toBeEnabled()
  })

  it('renders error state when lobby fetch fails', async () => {
    mockFetch
      .mockRejectedValueOnce(new TypeError('fetch failed'))
      .mockResolvedValueOnce(
        mockResponse({
          ok: true,
          status: 200,
          statusText: 'OK',
          jsonBody: agentsFixture,
        })
      )

    render(<ClawgicTournamentLobbyPage />)

    expect(await screen.findByText('Failed to load tournament lobby.')).toBeInTheDocument()
    expect(screen.getByText('Network error: fetch failed')).toBeInTheDocument()
  })

  it('submits tournament entry and shows success banner', async () => {
    mockFetch
      .mockResolvedValueOnce(
        mockResponse({
          ok: true,
          status: 200,
          statusText: 'OK',
          jsonBody: tournamentsFixture,
        })
      )
      .mockResolvedValueOnce(
        mockResponse({
          ok: true,
          status: 200,
          statusText: 'OK',
          jsonBody: agentsFixture,
        })
      )
      .mockResolvedValueOnce(
        mockResponse({
          ok: true,
          status: 201,
          statusText: 'Created',
          jsonBody: {
            entryId: '00000000-0000-0000-0000-000000000920',
            tournamentId: tournamentsFixture[0].tournamentId,
            agentId: agentsFixture[0].agentId,
            walletAddress: agentsFixture[0].walletAddress,
            status: 'CONFIRMED',
            seedSnapshotElo: 1000,
          },
        })
      )

    render(<ClawgicTournamentLobbyPage />)
    await screen.findByText('Debate on deterministic mocks')

    fireEvent.click(screen.getByRole('button', { name: 'Enter Tournament' }))

    expect(await screen.findByText(/entered successfully/i)).toBeInTheDocument()
    expect(mockFetch).toHaveBeenNthCalledWith(
      3,
      'http://localhost:8080/api/clawgic/tournaments/00000000-0000-0000-0000-000000000901/enter',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ agentId: '00000000-0000-0000-0000-000000000911' }),
      })
    )
  })

  it('shows full-state messaging when entry API returns capacity conflict', async () => {
    mockFetch
      .mockResolvedValueOnce(
        mockResponse({
          ok: true,
          status: 200,
          statusText: 'OK',
          jsonBody: tournamentsFixture,
        })
      )
      .mockResolvedValueOnce(
        mockResponse({
          ok: true,
          status: 200,
          statusText: 'OK',
          jsonBody: agentsFixture,
        })
      )
      .mockResolvedValueOnce(
        mockResponse({
          ok: false,
          status: 409,
          statusText: 'Conflict',
          textBody: JSON.stringify({
            detail: 'Tournament entry capacity reached: 00000000-0000-0000-0000-000000000901',
          }),
        })
      )

    render(<ClawgicTournamentLobbyPage />)
    await screen.findByText('Debate on deterministic mocks')

    fireEvent.click(screen.getByRole('button', { name: 'Enter Tournament' }))

    expect(
      await screen.findByText('Tournament is full. Choose another tournament or wait for a new round.')
    ).toBeInTheDocument()
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Enter Tournament' })).toBeDisabled()
    )
    expect(screen.getByText('Full')).toBeInTheDocument()
  })

  it('shows duplicate-entry messaging when entry API returns duplicate conflict', async () => {
    mockFetch
      .mockResolvedValueOnce(
        mockResponse({
          ok: true,
          status: 200,
          statusText: 'OK',
          jsonBody: tournamentsFixture,
        })
      )
      .mockResolvedValueOnce(
        mockResponse({
          ok: true,
          status: 200,
          statusText: 'OK',
          jsonBody: agentsFixture,
        })
      )
      .mockResolvedValueOnce(
        mockResponse({
          ok: false,
          status: 409,
          statusText: 'Conflict',
          textBody: JSON.stringify({
            detail: 'Agent is already entered in tournament: 00000000-0000-0000-0000-000000000901',
          }),
        })
      )

    render(<ClawgicTournamentLobbyPage />)
    await screen.findByText('Debate on deterministic mocks')

    fireEvent.click(screen.getByRole('button', { name: 'Enter Tournament' }))

    expect(
      await screen.findByText('This agent is already entered in the selected tournament.')
    ).toBeInTheDocument()
  })
})
