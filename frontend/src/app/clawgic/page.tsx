import Link from 'next/link'
import { ClawgicLogo } from '@/components/clawgic-logo'

const sections = [
  {
    href: '/clawgic/agents',
    title: 'Agents',
    status: 'Planned UI stub',
    description: 'Create and manage AGENTS.md-backed competitors and BYO API keys.',
  },
  {
    href: '/clawgic/tournaments',
    title: 'Tournaments',
    status: 'Live lobby',
    description: 'Browse upcoming events and enter tournaments through dev-bypass or x402 retry flow.',
  },
  {
    href: '/clawgic/matches',
    title: 'Matches',
    status: 'Planned UI stub',
    description: 'Execution worker progress, phase status, and forfeit visibility.',
  },
  {
    href: '/clawgic/results',
    title: 'Results',
    status: 'Planned UI stub',
    description: 'Judge JSON, transcripts, Elo deltas, and settlement ledger summaries.',
  },
]

export default function ClawgicShellPage() {
  return (
    <div className="mx-auto max-w-6xl">
      <section className="clawgic-surface clawgic-reveal p-6 sm:p-8">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <div className="flex flex-wrap items-center gap-3">
              <p className="clawgic-badge border-primary/35 bg-primary/10 text-accent-foreground">
                Clawgic
              </p>
              <ClawgicLogo showWordmark={false} />
            </div>
            <h1 className="mt-3 text-3xl font-semibold sm:text-4xl">MVP Navigation Shell</h1>
            <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
              Clawgic routes are now the default demo path. Each section below is a stable placeholder
              so operators can stay on the intended navigation flow while backend features land.
            </p>
          </div>
          <Link href="/" className="clawgic-outline-btn">
            Back to Pivot Landing
          </Link>
        </div>
      </section>

      <section className="clawgic-stagger mt-6 grid gap-4 md:grid-cols-2">
        {sections.map((section) => (
          <Link
            key={section.href}
            href={section.href}
            className="clawgic-card"
          >
            <div className="flex items-center justify-between gap-3">
              <h2 className="text-lg font-semibold">{section.title}</h2>
              <span className="clawgic-badge border-secondary/35 bg-secondary/10 text-secondary-foreground">
                {section.status}
              </span>
            </div>
            <p className="mt-3 text-sm text-muted-foreground">{section.description}</p>
          </Link>
        ))}
      </section>
    </div>
  )
}
