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
    { round: 1, scenario: 'high_bot', elo_stability: 0.4 },
    { round: 100, scenario: 'high_bot', elo_stability: 0.6 },
  ],
}

describe('SimulationPage', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows loading state initially', () => {
    vi.spyOn(global, 'fetch').mockReturnValue(new Promise(() => {}) as any)

    render(<SimulationPage />)

    expect(screen.getByText('Loading simulation data...')).toBeInTheDocument()
  })

  it('loads and renders simulation data with all charts', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSimulationData),
    } as Response)

    render(<SimulationPage />)

    await waitFor(() => {
      expect(screen.getByText('Simulation Playback')).toBeInTheDocument()
    })

    // Verify all 7 chart titles are rendered
    expect(screen.getByText('GlobalPool Balance Over Time')).toBeInTheDocument()
    expect(screen.getByText('Cumulative PnL by Agent Type')).toBeInTheDocument()
    expect(screen.getByText('New Market Bootstrap Curve')).toBeInTheDocument()
    expect(screen.getByText('Alpha Sensitivity Analysis')).toBeInTheDocument()
    expect(screen.getByText('Minority Loss Sensitivity')).toBeInTheDocument()
    expect(screen.getByText('Audit Pair Detection Rate')).toBeInTheDocument()
    expect(screen.getByText('Bot vs Human Earnings')).toBeInTheDocument()
  })

  it('renders chart containers', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSimulationData),
    } as Response)

    render(<SimulationPage />)

    await waitFor(() => {
      const lineCharts = screen.getAllByTestId('line-chart')
      expect(lineCharts.length).toBeGreaterThanOrEqual(5)
    })

    const barCharts = screen.getAllByTestId('bar-chart')
    expect(barCharts.length).toBeGreaterThanOrEqual(1)
  })

  it('shows error state when fetch fails', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: false,
    } as Response)

    render(<SimulationPage />)

    await waitFor(() => {
      expect(screen.getByText('Error loading simulation data')).toBeInTheDocument()
    })
  })

  it('shows error on network failure', async () => {
    vi.spyOn(global, 'fetch').mockRejectedValue(new Error('Network error'))

    render(<SimulationPage />)

    await waitFor(() => {
      expect(screen.getByText('Error loading simulation data')).toBeInTheDocument()
    })
  })

  it('renders scenario selector with options from data', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSimulationData),
    } as Response)

    render(<SimulationPage />)

    await waitFor(() => {
      expect(screen.getByText('Scenario:')).toBeInTheDocument()
    })

    // Verify scenario options
    const select = screen.getByRole('combobox')
    expect(select).toBeInTheDocument()
  })

  it('renders Feed Quality chart with scenario filtering', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSimulationData),
    } as Response)

    render(<SimulationPage />)

    await waitFor(() => {
      expect(screen.getByText('Feed Quality / ELO Stability')).toBeInTheDocument()
    })

    // Default scenario is 'baseline' - verify description text references it
    expect(
      screen.getByText(/ELO stability over time for baseline scenario/)
    ).toBeInTheDocument()
  })

  it('changes scenario on selector change', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSimulationData),
    } as Response)

    render(<SimulationPage />)

    await waitFor(() => {
      expect(screen.getByText('Scenario:')).toBeInTheDocument()
    })

    // Change scenario
    const select = screen.getByRole('combobox')
    fireEvent.change(select, { target: { value: 'high_bot' } })

    await waitFor(() => {
      expect(
        screen.getByText(/ELO stability over time for high.bot scenario/)
      ).toBeInTheDocument()
    })
  })

  it('fetches data from /simulation/results.json', async () => {
    const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSimulationData),
    } as Response)

    render(<SimulationPage />)

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith('/simulation/results.json')
    })
  })

  it('renders description text for charts', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSimulationData),
    } as Response)

    render(<SimulationPage />)

    await waitFor(() => {
      expect(
        screen.getByText(/Solvency proof: Pool balance remains stable/i)
      ).toBeInTheDocument()
      expect(
        screen.getByText(/Expected hierarchy: Honest/i)
      ).toBeInTheDocument()
    })
  })

  it('renders footer with PRD reference', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockSimulationData),
    } as Response)

    render(<SimulationPage />)

    await waitFor(() => {
      expect(
        screen.getByText(/PRD Sections 6.4, 8.5, 11/)
      ).toBeInTheDocument()
    })
  })
})
