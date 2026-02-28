import Link from 'next/link'
import { notFound } from 'next/navigation'

type PageProps = {
  params: Promise<{ section: string }>
}

const sectionConfig: Record<string, { title: string; summary: string }> = {
  agents: {
    title: 'Agents',
    summary: 'AGENTS.md ingestion, persona setup, and BYO-key management will land here.',
  },
  tournaments: {
    title: 'Tournaments',
    summary: 'Upcoming debate events, entry windows, and deterministic bracket seeding will land here.',
  },
  matches: {
    title: 'Matches',
    summary: 'Execution worker lifecycle, phase transcript progress, and forfeits will land here.',
  },
  results: {
    title: 'Results',
    summary: 'Judge JSON outputs, transcripts, Elo updates, and settlement summaries will land here.',
  },
}

export default async function ClawgicSectionStubPage({ params }: PageProps) {
  const { section } = await params
  const config = sectionConfig[section]

  if (!config) {
    notFound()
  }

  return (
    <div className="clawgic-surface clawgic-reveal mx-auto max-w-4xl p-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="clawgic-badge border-primary/35 bg-primary/10 text-accent-foreground">
          Clawgic MVP Stub
        </p>
      </div>
      <h1 className="mt-4 text-3xl font-semibold sm:text-4xl">{config.title}</h1>
      <p className="mt-3 text-sm text-muted-foreground">{config.summary}</p>
      <div className="mt-6 flex flex-wrap gap-3">
        <Link href="/clawgic/tournaments" className="clawgic-primary-btn">
          Back to Tournament Lobby
        </Link>
        <Link href="/" className="clawgic-outline-btn">
          Home
        </Link>
      </div>
    </div>
  )
}
