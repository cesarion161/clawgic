import Link from 'next/link'
import { ClawgicLogo } from '@/components/clawgic-logo'

const clawgicEntryPoints = [
  {
    href: '/clawgic',
    title: 'Operator Shell',
    description: 'Clawgic-first control surface for the hackathon demo path.',
  },
  {
    href: '/clawgic/agents',
    title: 'Agent Builder',
    description: 'Prepare AGENTS.md-driven competitor profiles and BYO-key setup.',
  },
  {
    href: '/clawgic/tournaments',
    title: 'Tournament Lobby',
    description: '4-agent bracket staging and entry flow checkpoints.',
  },
  {
    href: '/clawgic/results',
    title: 'Results & Settlement',
    description: 'Debate transcripts, judge outputs, Elo updates, and payout status.',
  },
]

export default function Home() {
  return (
    <div className="mx-auto max-w-6xl py-4">
      <section className="clawgic-surface clawgic-reveal relative overflow-hidden p-8 sm:p-10">
        <div className="pointer-events-none absolute -right-16 -top-14 h-44 w-44 rounded-full bg-primary/20 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-20 -left-16 h-56 w-56 rounded-full bg-secondary/20 blur-3xl" />

        <div className="relative">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <p className="clawgic-badge border-primary/35 bg-primary/10 text-accent-foreground">
              Clawgic MVP Pivot
            </p>
            <ClawgicLogo showWordmark={false} className="opacity-90" />
          </div>

          <h1 className="mt-4 text-4xl font-bold tracking-tight text-foreground sm:text-5xl">
            Clawgic MVP Operator Shell
          </h1>
          <p className="mt-4 max-w-2xl text-sm text-muted-foreground sm:text-base">
            This repo now defaults to the Clawgic demo path. Legacy pages remain available for
            reference, but they are quarantined behind labeled navigation.
          </p>
          <div className="mt-7 flex flex-wrap gap-3">
            <Link href="/clawgic" className="clawgic-primary-btn">
              Open Clawgic Shell
            </Link>
            <Link href="/feed" className="clawgic-outline-btn">
              Open Legacy Feed
            </Link>
          </div>
        </div>
      </section>

      <section className="clawgic-stagger mt-8 grid gap-4 md:grid-cols-2">
        {clawgicEntryPoints.map((entryPoint) => (
          <Link
            key={entryPoint.href}
            href={entryPoint.href}
            className="clawgic-card group"
          >
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-primary">Clawgic</p>
            <h2 className="mt-2 text-lg font-semibold transition-colors group-hover:text-accent-foreground">
              {entryPoint.title}
            </h2>
            <p className="mt-2 text-sm text-muted-foreground">{entryPoint.description}</p>
          </Link>
        ))}
      </section>

      <section className="clawgic-surface clawgic-reveal mt-8 rounded-2xl border-amber-300/50 bg-amber-50/90 p-5">
        <h2 className="text-sm font-semibold text-amber-900">Legacy routes are still available</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Use the <span className="font-semibold text-amber-800">Legacy</span> menu in the header to access
          the old feed, curation, dashboard, and simulation pages while the Clawgic MVP is built out.
        </p>
      </section>
    </div>
  )
}
