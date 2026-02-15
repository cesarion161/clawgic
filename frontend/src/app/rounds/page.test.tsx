import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import RoundsPage from './page'

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
    market: { id: 1, name: 'Tech' },
    status: 'SETTLED',
    pairs: 10,
    basePerPair: 5000000000,
    premiumPerPair: 2000000000,
    contentMerkleRoot: '0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890',
    startedAt: '2026-02-10T12:00:00Z',
    commitDeadline: '2026-02-10T13:00:00Z',
    revealDeadline: '2026-02-10T14:00:00Z',
    settledAt: '2026-02-10T15:00:00Z',
    createdAt: '2026-02-10T11:00:00Z',
  },
  {
    id: 2,
    market: { id: 1, name: 'Tech' },
    status: 'OPEN',
    pairs: 5,
    basePerPair: 3000000000,
    premiumPerPair: 1000000000,
    contentMerkleRoot: null,
    startedAt: '2026-02-15T10:00:00Z',
    commitDeadline: null,
    revealDeadline: null,
    settledAt: null,
    createdAt: '2026-02-15T09:00:00Z',
  },
  {
    id: 3,
    market: { id: 1, name: 'Tech' },
    status: 'COMMIT',
    pairs: 8,
    basePerPair: 4000000000,
    premiumPerPair: 1500000000,
    contentMerkleRoot: null,
    startedAt: '2026-02-14T10:00:00Z',
    commitDeadline: '2026-02-14T12:00:00Z',
    revealDeadline: null,
    settledAt: null,
    createdAt: '2026-02-14T09:00:00Z',
  },
]

describe('RoundsPage', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows loading state initially', () => {
    mockGet.mockReturnValue(new Promise(() => {})) // never resolves

    render(<RoundsPage />)

    expect(screen.getByText('Loading rounds...')).toBeInTheDocument()
  })

  it('renders rounds table with data', async () => {
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
    expect(screen.getByText('SETTLED')).toBeInTheDocument()
    expect(screen.getByText('OPEN')).toBeInTheDocument()
    expect(screen.getByText('COMMIT')).toBeInTheDocument()
  })

  it('shows error state on API failure', async () => {
    mockGet.mockRejectedValue(new Error('Network error'))

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('Failed to load rounds')).toBeInTheDocument()
    })
  })

  it('shows "No rounds found" when list is empty', async () => {
    mockGet.mockResolvedValue([])

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('No rounds found')).toBeInTheDocument()
    })
  })

  it('calculates total stake correctly in SURGE', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      // Round 1: (5000000000 + 2000000000) * 10 / 1e9 = 70.00 SURGE
      expect(screen.getByText('70.00 SURGE')).toBeInTheDocument()
    })
  })

  it('displays truncated settlement hash', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('0xabcdef12...')).toBeInTheDocument()
    })
  })

  it('shows N/A for rounds without settlement hash', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      const naElements = screen.getAllByText('N/A')
      expect(naElements.length).toBeGreaterThan(0)
    })
  })

  it('renders status filter dropdown', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('Status:')).toBeInTheDocument()
    })

    // Verify filter options exist
    const statusSelect = screen.getAllByRole('combobox')[1]
    expect(statusSelect).toBeInTheDocument()
  })

  it('renders market filter dropdown', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('Market:')).toBeInTheDocument()
    })
  })

  it('expands round details on row click', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('#1')).toBeInTheDocument()
    })

    // Click on the first row to expand
    fireEvent.click(screen.getByText('#1'))

    // Should show round details
    await waitFor(() => {
      expect(screen.getByText('Round Details')).toBeInTheDocument()
    })
  })

  it('filters rounds by status', async () => {
    mockGet.mockResolvedValue(mockRounds)

    render(<RoundsPage />)

    await waitFor(() => {
      expect(screen.getByText('#1')).toBeInTheDocument()
    })

    // Filter by SETTLED status
    const statusSelect = screen.getAllByRole('combobox')[1]
    fireEvent.change(statusSelect, { target: { value: 'SETTLED' } })

    // Should only show settled round
    expect(screen.getByText('#1')).toBeInTheDocument()
    expect(screen.queryByText('#2')).not.toBeInTheDocument()
    expect(screen.queryByText('#3')).not.toBeInTheDocument()
  })

  it('calls API with market ID parameter', async () => {
    mockGet.mockResolvedValue([])

    render(<RoundsPage />)

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith('/rounds?marketId=1')
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
