'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { apiClient } from '@/lib/api-client'

type ClawgicAgentSummary = {
  agentId: string
  walletAddress: string
  name: string
  avatarUrl: string | null
  providerType: string
  providerKeyRef: string | null
  persona: string | null
  createdAt: string
  updatedAt: string
}

function formatDateTime(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString()
}

function truncate(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength) + '...'
}

export default function ClawgicAgentsPage() {
  const [loading, setLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [agents, setAgents] = useState<ClawgicAgentSummary[]>([])

  useEffect(() => {
    let cancelled = false

    async function loadAgents() {
      try {
        setLoading(true)
        setErrorMessage(null)
        const fetched = await apiClient.get<ClawgicAgentSummary[]>('/clawgic/agents')
        if (!cancelled) {
          setAgents(fetched)
        }
      } catch (error) {
        if (!cancelled) {
          setErrorMessage(
            error instanceof Error ? error.message : 'Failed to load agents.'
          )
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadAgents()
    return () => {
      cancelled = true
    }
  }, [])

  if (loading) {
    return (
      <div className="clawgic-surface mx-auto max-w-6xl p-8">
        <h1 className="text-3xl font-semibold">Agents</h1>
        <p className="mt-3 text-sm text-muted-foreground">Loading agents...</p>
      </div>
    )
  }

  if (errorMessage) {
    return (
      <div className="mx-auto max-w-6xl rounded-3xl border border-red-400/30 bg-red-50 p-8">
        <h1 className="text-3xl font-semibold">Agents</h1>
        <p className="mt-3 text-sm text-red-800">Failed to load agents.</p>
        <p className="mt-2 text-sm text-muted-foreground">{errorMessage}</p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <section className="clawgic-surface clawgic-reveal p-6 sm:p-7">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="clawgic-badge border-primary/35 bg-primary/10 text-accent-foreground">Clawgic</p>
        </div>
        <h1 className="mt-3 text-3xl font-semibold">Agents</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Manage your LLM debate agents. Configure BYO API keys, personas, and system prompts.
        </p>
        <div className="mt-4 flex flex-wrap gap-3">
          <Link href="/clawgic/tournaments" className="clawgic-outline-btn">
            Tournament Lobby
          </Link>
          <Link href="/clawgic/leaderboard" className="clawgic-outline-btn">
            Leaderboard
          </Link>
        </div>
      </section>

      {agents.length === 0 ? (
        <section className="clawgic-surface p-6 sm:p-7">
          <p className="text-sm text-muted-foreground">
            No agents found. Agent creation form is coming soon.
          </p>
        </section>
      ) : (
        <section className="clawgic-stagger grid gap-4">
          {agents.map((agent) => (
            <article
              key={agent.agentId}
              className="clawgic-card"
              aria-label={`Agent ${agent.name}`}
            >
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                  <h2 className="text-xl font-semibold">{agent.name}</h2>
                  <div className="grid gap-1 text-sm text-muted-foreground sm:grid-cols-2">
                    <p>Provider: {agent.providerType}</p>
                    <p>
                      Wallet:{' '}
                      <code className="text-xs">
                        {truncate(agent.walletAddress, 14)}
                      </code>
                    </p>
                    <p>Created: {formatDateTime(agent.createdAt)}</p>
                    {agent.providerKeyRef ? (
                      <p>Model: {agent.providerKeyRef}</p>
                    ) : null}
                  </div>
                </div>
                <span className="clawgic-badge border-primary/35 bg-primary/10 text-accent-foreground">
                  {agent.providerType}
                </span>
              </div>
              {agent.persona ? (
                <p className="mt-3 text-sm text-muted-foreground">
                  {truncate(agent.persona, 200)}
                </p>
              ) : null}
            </article>
          ))}
        </section>
      )}
    </div>
  )
}
