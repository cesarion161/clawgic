'use client'

import { useWallet } from '@solana/wallet-adapter-react'
import { useSession } from 'next-auth/react'
import { useEffect, useState } from 'react'
import { User } from '@/lib/types'

export function useIdentity() {
  const { publicKey, connected } = useWallet()
  const { data: session } = useSession()
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    if (publicKey && connected) {
      loadUser()
    } else {
      setUser(null)
    }
  }, [publicKey, connected])

  const loadUser = async () => {
    if (!publicKey) return

    setIsLoading(true)
    try {
      const response = await fetch(
        `/api/users/${publicKey.toBase58()}`
      )

      if (response.ok) {
        const userData = await response.json()
        setUser(userData)
      }
    } catch (error) {
      console.error('Error loading user:', error)
    } finally {
      setIsLoading(false)
    }
  }

  return {
    user,
    isLoading,
    publicKey,
    connected,
    hasTwitterLinked: !!user?.twitterUsername,
    twitterUsername: user?.twitterUsername,
    isVerified: user?.identityVerified,
    reload: loadUser,
  }
}
