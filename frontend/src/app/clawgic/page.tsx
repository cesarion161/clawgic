import { redirect } from 'next/navigation'

export default function ClawgicLegacyShellPage() {
  // Legacy early-stage shell route retained only for backward compatibility.
  // The user-facing production path starts at the tournament lobby.
  redirect('/clawgic/tournaments')
}
