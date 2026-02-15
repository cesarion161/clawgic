import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import SimulationPage from './page'

// Mock recharts to avoid SVG rendering issues in jsdom
vi.mock('recharts', () => ({
  LineChart: ({ children }: any) => <div data-testid="line-chart">{children}</div>,
  Line: () => null,
  BarChart: ({ children }: any) => <div data-testid="bar-chart">{children}</div>,
  Bar: () => null,
  XAxis: () => null,
  YAxis: () => null,
  CartesianGrid: () => null,
  Tooltip: () => null,
  Legend: () => null,
  ResponsiveContainer: ({ children }: any) => <div>{children}</div>,
}))

const mockSimulationData = {
  chart_1_pool_balance: [
    { round: 1, balance: 1000000 },
    { round: 100, balance: 1050000 },
    { round: 200, balance: 1100000 },
  ],
  chart_2_cumulative_pnl: [
    { round: 1, agent_type: 'honest', cumulative_pnl: 100 },
    { round: 1, agent_type: 'random', cumulative_pnl: -20 },
    { round: 1, agent_type: 'lazy', cumulative_pnl: -50 },
    { round: 100, agent_type: 'honest', cumulative_pnl: 5000 },
    { round: 100, agent_type: 'random', cumulative_pnl: -500 },
    { round: 100, agent_type: 'lazy', cumulative_pnl: -2000 },
  ],
  chart_3_new_market_bootstrap: [
    { round: 1, curators_attracted: 2 },
    { round: 10, curators_attracted: 15 },
  ],
  chart_4a_alpha_sensitivity: [
    { alpha: 0.1, pool_balance: 800000, avg_curator_rewards: 200 },
    { alpha: 0.3, pool_balance: 1000000, avg_curator_rewards: 300 },
    { alpha: 0.5, pool_balance: 700000, avg_curator_rewards: 500 },
  ],
  chart_4b_minority_loss_sensitivity: [
    { minority_loss_pct: 10, consensus_alignment: 0.6, avg_pnl_loss: -50 },
    { minority_loss_pct: 20, consensus_alignment: 0.75, avg_pnl_loss: -100 },
  ],
  chart_5_audit_detection: [
    { evaluations: 10, lazy_detection_rate: 0.3, bot_detection_rate: 0.2 },
    { evaluations: 50, lazy_detection_rate: 0.8, bot_detection_rate: 0.7 },
  ],
  chart_6_bot_vs_human: [
    { bot_percentage: 10, human_avg_earnings: 450, bot_avg_earnings: -200 },
    { bot_percentage: 30, human_avg_earnings: 350, bot_avg_earnings: -400 },
  ],
  chart_7_feed_quality: [
    { round: 1, scenario: 'baseline', elo_stability: 0.5 },
    { round: 100, scenario: 'baseline', elo_stability: 0.9 },
    { round: 1, scenario: 'bot_flood', elo_stability: 0.4 },
    { round: 100, scenario: 'bot_flood', elo_stability: 0.6 },
  ],
}

describe('SimulationPage', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    vi.spyOn(console, 'warn').mockImplementation(() => {})
    fetchMock = vi.fn()
    globalThis.fetch = fetchMock
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows loading state initially', () => {
    fetchMock.mockReturnValue(new Promise(() => {}))

    render(<SimulationPage />)

    expect(screen.getByText('Loading simulation data...')).toBeInTheDocument()
  })

  it('shows loading spinner', () => {
    fetchMock.mockReturnValue(new Promise(() => {}))

    render(<SimulationPage />)

    const spinner = document.querySelector('.animate-spin')
    expect(spinner).not.toBeNull()
  })

  it('renders page header after loading', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSimulationData),
    })

    render(<SimulationPage />)

    await waitFor(() => {
      expect(screen.getByText('Simulation Playback')).toBeInTheDocument()
    })

    expect(screen.getByText(/Reproducible results from 10,000-round simulation/)).toBeInTheDocument()
  })

  it('shows error state when fetch fails', async () => {
    fetchMock.mockResolvedValue({
      ok: false,
      status: 404,
    })

    render(<SimulationPage />)

    await waitFor(() => {
      expect(screen.getByText('Error loading simulation data')).toBeInTheDocument()
    })

    expect(screen.getByText('Failed to load simulation data')).toBeInTheDocument()
  })

  it('shows error state on network failure', async () => {
    fetchMock.mockRejectedValue(new Error('Network error'))

    render(<SimulationPage />)

    await waitFor(() => {
      expect(screen.getByText('Error loading simulation data')).toBeInTheDocument()
    })

    expect(screen.getByText('Network error')).toBeInTheDocument()
  })

  it('renders all chart titles', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSimulationData),
    })

    render(<SimulationPage />)

    await waitFor(() => {
      expect(screen.getByText('GlobalPool Balance Over Time')).toBeInTheDocument()
    })

    expect(screen.getByText('Cumulative PnL by Agent Type')).toBeInTheDocument()
    expect(screen.getByText('New Market Bootstrap Curve')).toBeInTheDocument()
    expect(screen.getByText('Alpha Sensitivity Analysis')).toBeInTheDocument()
    expect(screen.getByText('Minority Loss Sensitivity')).toBeInTheDocument()
    expect(screen.getByText('Audit Pair Detection Rate')).toBeInTheDocument()
    expect(screen.getByText('Bot vs Human Earnings')).toBeInTheDocument()
    expect(screen.getByText('Feed Quality / ELO Stability')).toBeInTheDocument()
  })

  it('renders chart descriptions', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSimulationData),
    })

    render(<SimulationPage />)

    await waitFor(() => {
      expect(screen.getByText(/Solvency proof/)).toBeInTheDocument()
    })

    expect(screen.getByText(/Expected hierarchy/)).toBeInTheDocument()
    expect(screen.getByText(/Curators attracted/)).toBeInTheDocument()
  })

  it('renders line chart and bar chart components', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSimulationData),
    })

    render(<SimulationPage />)

    await waitFor(() => {
      const lineCharts = screen.getAllByTestId('line-chart')
      expect(lineCharts.length).toBeGreaterThanOrEqual(6)
    })

    const barCharts = screen.getAllByTestId('bar-chart')
    expect(barCharts.length).toBe(1)
  })

  it('renders scenario selector with options from data', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSimulationData),
    })

    render(<SimulationPage />)

    await waitFor(() => {
      expect(screen.getByText('Scenario:')).toBeInTheDocument()
    })

    // Check scenarios are listed (derived from chart_7_feed_quality)
    expect(screen.getByText('Baseline')).toBeInTheDocument()
    expect(screen.getByText('Bot Flood')).toBeInTheDocument()
  })

  it('changes scenario selection', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSimulationData),
    })

    render(<SimulationPage />)

    await waitFor(() => {
      expect(screen.getByText('Scenario:')).toBeInTheDocument()
    })

    const select = screen.getByRole('combobox')
    fireEvent.change(select, { target: { value: 'bot_flood' } })

    // Feed Quality chart description should reference the selected scenario
    await waitFor(() => {
      expect(screen.getByText(/bot flood scenario/)).toBeInTheDocument()
    })
  })

  it('fetches from correct URL', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSimulationData),
    })

    render(<SimulationPage />)

    expect(fetchMock).toHaveBeenCalledWith('/simulation/results.json')
  })

  it('renders footer with PRD reference', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSimulationData),
    })

    render(<SimulationPage />)

    await waitFor(() => {
      expect(screen.getByText(/Not screenshot artifacts/)).toBeInTheDocument()
    })

    expect(screen.getByText(/PRD Sections/)).toBeInTheDocument()
  })

  it('handles empty simulation data gracefully', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(null),
    })

    render(<SimulationPage />)

    await waitFor(() => {
      expect(screen.getByText('No simulation data available')).toBeInTheDocument()
    })
  })
})
