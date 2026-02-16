import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import RoundsPage from './page'
import { RoundStatus } from '@/lib/types'

// Mock the API client module
const mockGet = vi.fn()

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    get: (...args: any[]) => mockGet(...args),
  },
}))

const mockRounds = [
  {
    id: 1,
    market: { id: 1, name: 'AI Tech' },
    status: RoundStatus.SETTLED,
    pairs: 20,
    basePerPair: 150_000_000,
    premiumPerPair: 350_000_000,
    contentMerkleRoot: 'abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890',
    startedAt: '2026-02-14T10:00:00Z',
    commitDeadline: '2026-02-14T11:00:00Z',
    revealDeadline: '2026-02-14T12:00:00Z',
    settledAt: '2026-02-14T13:00:00Z',
    createdAt: '2026-02-14T09:00:00Z',
    updatedAt: '2026-02-14T13:00:00Z',
  },
  {
    id: 2,
    market: { id: 1, name: 'AI Tech' },
    status: RoundStatus.COMMIT,
    pairs: 15,
    basePerPair: 100_000_000,
    premiumPerPair: 200_000_000,
    contentMerkleRoot: null,
    startedAt: '2026-02-15T10:00:00Z',
    commitDeadline: '2026-02-15T11:00:00Z',
    revealDeadline: null,
    settledAt: null,
    createdAt: '2026-02-15T09:00:00Z',
    updatedAt: '2026-02-15T10:00:00Z',
  },
  {
    id: 3,
    market: { id: 1, name: 'AI Tech' },
    status: RoundStatus.OPEN,
    pairs: 0,
    basePerPair: 0,
    premiumPerPair: 0,
    contentMerkleRoot: null,
    startedAt: null,
    commitDeadline: null,
    revealDeadline: null,
    settledAt: null,
    createdAt: '2026-02-15T12:00:00Z',
    updatedAt: '2026-02-15T12:00:00Z',
  },
]

describe('RoundsPage', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    vi.spyOn(console, 'warn').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows loading state initially', () => {
    mockGet.mockReturnValue(new Promise(() => {}))

    render(<RoundsPage />)

    expect(screen.getByText('Loading rounds...')).toBeInTheDocument()
  })

  it('renders rounds table after loading', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('Rounds Archive')).toBeInTheDocument()
    })

    // Verify table headers
    expect(screen.getByText('Round ID')).toBeInTheDocument()
    expect(screen.getByText('Market')).toBeInTheDocument()
    expect(screen.getByText('Status')).toBeInTheDocument()
    expect(screen.getByText('Pairs')).toBeInTheDocument()
    expect(screen.getByText('Total Stake')).toBeInTheDocument()

    // Verify round data rendered
    expect(screen.getByText('#1')).toBeInTheDocument()
    expect(screen.getByText('#2')).toBeInTheDocument()
    expect(screen.getByText('#3')).toBeInTheDocument()
  })

  it('shows error state on API failure', async () => {
    mockGet.mockRejectedValue(new Error('Server error'))

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('Failed to load rounds')).toBeInTheDocument()
    })
  })

  it('displays round statuses with correct text', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('SETTLED')).toBeInTheDocument()
    })

    expect(screen.getByText('COMMIT')).toBeInTheDocument()
    expect(screen.getByText('OPEN')).toBeInTheDocument()
  })

  it('displays market names', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getAllByText('AI Tech')).toHaveLength(3)
    })
  })

  it('calculates total stake correctly', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      // Round 1: (150M + 350M) * 20 = 10B lamports = 10.00 SURGE
      expect(screen.getByText('10.00 SURGE')).toBeInTheDocument()
    })

    // Round 2: (100M + 200M) * 15 = 4.5B lamports = 4.50 SURGE
    expect(screen.getByText('4.50 SURGE')).toBeInTheDocument()
  })

  it('shows truncated settlement hash for settled rounds', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('abcdef1234...')).toBeInTheDocument()
    })
  })

  it('shows N/A for rounds without settlement hash', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getAllByText('N/A').length).toBeGreaterThanOrEqual(1)
    })
  })

  it('shows Copy button for rounds with settlement hash', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('Copy')).toBeInTheDocument()
    })
  })

  it('shows empty state when no rounds match filter', async () => {
    mockGet.mockResolvedValue([])

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('No rounds found')).toBeInTheDocument()
    })
  })

  it('renders market filter select', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('Rounds Archive')).toBeInTheDocument()
    })

    expect(screen.getByText('Market 1')).toBeInTheDocument()
    expect(screen.getByText('Market 2')).toBeInTheDocument()
    expect(screen.getByText('Market 3')).toBeInTheDocument()
  })

  it('renders status filter select', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('Rounds Archive')).toBeInTheDocument()
    })

    expect(screen.getByText('All')).toBeInTheDocument()
    expect(screen.getByText('Settled')).toBeInTheDocument()
    expect(screen.getByText('Settling')).toBeInTheDocument()
    expect(screen.getByText('Reveal')).toBeInTheDocument()
    expect(screen.getByText('Commit', { selector: 'option' })).toBeInTheDocument()
    expect(screen.getByText('Open')).toBeInTheDocument()
  })

  it('filters rounds by status', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('#1')).toBeInTheDocument()
    })

    // All 3 rounds visible initially
    expect(screen.getByText('#1')).toBeInTheDocument()
    expect(screen.getByText('#2')).toBeInTheDocument()
    expect(screen.getByText('#3')).toBeInTheDocument()

    // Filter to SETTLED only
    const statusSelect = screen.getAllByRole('combobox')[1]
    fireEvent.change(statusSelect, { target: { value: 'SETTLED' } })

    expect(screen.getByText('#1')).toBeInTheDocument()
    expect(screen.queryByText('#2')).not.toBeInTheDocument()
    expect(screen.queryByText('#3')).not.toBeInTheDocument()
  })

  it('changes market filter triggers API call', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith('/rounds?marketId=1')
    })

    const marketSelect = screen.getAllByRole('combobox')[0]
    fireEvent.change(marketSelect, { target: { value: '2' } })

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith('/rounds?marketId=2')
    })
  })

  it('expands row to show round details on click', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('#1')).toBeInTheDocument()
    })

    // Click on first round row
    fireEvent.click(screen.getByText('#1'))

    await waitFor(() => {
      expect(screen.getByText('Round Details')).toBeInTheDocument()
    })

    // Should show detailed info
    expect(screen.getByText('Created:')).toBeInTheDocument()
    expect(screen.getByText('Started:')).toBeInTheDocument()
    expect(screen.getByText('Commit Deadline:')).toBeInTheDocument()
    expect(screen.getByText('Reveal Deadline:')).toBeInTheDocument()
  })

  it('collapses expanded row on second click', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('#1')).toBeInTheDocument()
    })

    // Expand
    fireEvent.click(screen.getByText('#1'))
    await waitFor(() => {
      expect(screen.getByText('Round Details')).toBeInTheDocument()
    })

    // Collapse
    fireEvent.click(screen.getByText('#1'))
    expect(screen.queryByText('Round Details')).not.toBeInTheDocument()
  })

  it('displays table headers', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('Round ID')).toBeInTheDocument()
    })

    expect(screen.getByText('Market')).toBeInTheDocument()
    expect(screen.getByText('Status')).toBeInTheDocument()
    expect(screen.getByText('Pairs')).toBeInTheDocument()
    expect(screen.getByText('Total Stake')).toBeInTheDocument()
    expect(screen.getByText('Settlement Hash')).toBeInTheDocument()
    expect(screen.getByText('Actions')).toBeInTheDocument()
  })

  it('shows pairs count', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('20')).toBeInTheDocument()
    })

    expect(screen.getByText('15')).toBeInTheDocument()
  })

  it('handles missing market gracefully', async () => {
    const roundWithoutMarket = [{
      ...mockRounds[0],
      market: undefined,
    }]
    mockGet.mockResolvedValue(roundWithoutMarket)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('Unknown Market')).toBeInTheDocument()
    })
  })

  it('renders without crashing with diverse round statuses', async () => {
    const allStatuses = [
      { ...mockRounds[0], id: 1, status: 'OPEN' },
      { ...mockRounds[0], id: 2, status: 'COMMIT' },
      { ...mockRounds[0], id: 3, status: 'REVEAL' },
      { ...mockRounds[0], id: 4, status: 'SETTLING' },
      { ...mockRounds[0], id: 5, status: 'SETTLED' },
    ]
    mockGet.mockResolvedValue(allStatuses)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('#1')).toBeInTheDocument()
      expect(screen.getByText('#5')).toBeInTheDocument()
    })
  })
})
