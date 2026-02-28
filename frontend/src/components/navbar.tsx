'use client'

import Link from 'next/link'
import { WalletMultiButton } from '@solana/wallet-adapter-react-ui'
import { LinkTwitter } from './link-twitter'
import { ClawgicLogo } from './clawgic-logo'

export function Navbar() {
  const clawgicLinks = [
    { href: '/', label: 'Clawgic Home' },
    { href: '/clawgic', label: 'Shell' },
    { href: '/clawgic/agents', label: 'Agents' },
    { href: '/clawgic/tournaments', label: 'Tournaments' },
    { href: '/clawgic/results', label: 'Results' },
  ]

  const legacyLinks = [
    { href: '/feed', label: 'Feed' },
    { href: '/curate', label: 'Curate' },
    { href: '/dashboard', label: 'Dashboard' },
    { href: '/leaderboard', label: 'Leaderboard' },
    { href: '/pool', label: 'Pool' },
    { href: '/simulation', label: 'Simulation' },
    { href: '/rounds', label: 'Rounds' },
  ]

  return (
    <nav className="clawgic-nav-blur sticky top-0 z-40">
      <div className="container mx-auto px-4">
        <div className="flex min-h-[5.25rem] flex-wrap items-center justify-between gap-4 py-3">
          <div className="flex items-center gap-4">
            <Link
              href="/"
              className="group rounded-xl px-1 py-1 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/60 focus-visible:ring-offset-2 focus-visible:ring-offset-white"
              aria-label="Clawgic"
            >
              <ClawgicLogo
                labelClassName="text-xl transition-colors group-hover:text-accent-foreground"
                markClassName="transition-transform duration-200 group-hover:scale-[1.04]"
              />
            </Link>

            <span className="clawgic-badge border-secondary/40 bg-secondary/15 text-secondary-foreground">
              MVP
            </span>
          </div>

          <div className="order-3 w-full overflow-x-auto pb-1 md:order-none md:w-auto md:pb-0">
            <div className="flex min-w-max items-center gap-4 pr-2 md:gap-5">
              {clawgicLinks.map((link) => (
                <Link
                  key={link.href}
                  href={link.href}
                  className="rounded-lg px-2 py-1 text-sm font-medium text-foreground/85 transition-all duration-200 hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/60"
                >
                  {link.label}
                </Link>
              ))}
              <details className="relative">
                <summary className="cursor-pointer list-none rounded-lg px-2 py-1 text-sm font-medium text-amber-700 transition-all duration-200 hover:bg-amber-100 hover:text-amber-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amber-500/60">
                  Legacy
                </summary>
                <div className="absolute left-0 top-full z-30 mt-2 w-64 rounded-xl border border-border bg-white/95 p-2 shadow-xl backdrop-blur">
                  <p className="px-2 py-1 text-xs text-muted-foreground/90">
                    Preserved for reference while Clawgic MVP ships.
                  </p>
                  <div className="mt-1 space-y-1">
                    {legacyLinks.map((link) => (
                      <Link
                        key={link.href}
                        href={link.href}
                        className="flex items-center justify-between rounded-md px-2 py-1.5 text-sm text-muted-foreground transition-colors hover:bg-muted/80 hover:text-foreground"
                      >
                        <span>{link.label}</span>
                        <span className="text-[10px] uppercase tracking-wide text-amber-600">
                          Legacy
                        </span>
                      </Link>
                    ))}
                  </div>
                </div>
              </details>
            </div>
          </div>

          <div className="flex items-center gap-2 sm:gap-3">
            <LinkTwitter />
            <WalletMultiButton />
          </div>
        </div>
      </div>
    </nav>
  )
}
