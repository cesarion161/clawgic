'use client'

import Image from 'next/image'
import { useEffect, useRef } from 'react'
import { cn } from '@/lib/utils'

type HeroParallaxLogoProps = {
  className?: string
}

export function HeroParallaxLogo({ className }: HeroParallaxLogoProps) {
  const containerRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    let frame = 0

    const updateParallax = () => {
      frame = 0
      const element = containerRef.current
      if (!element) {
        return
      }

      const host = element.parentElement
      if (!host) {
        return
      }

      const rect = host.getBoundingClientRect()
      const viewportHeight = window.innerHeight || 1
      const start = viewportHeight
      const end = -rect.height
      const progressRaw = (start - rect.top) / (start - end)
      const progress = Math.max(0, Math.min(1, progressRaw))
      const shift = (progress - 0.5) * 120
      element.style.setProperty('--parallax-shift', `${shift.toFixed(1)}px`)
    }

    const onScroll = () => {
      if (frame !== 0) {
        return
      }
      frame = window.requestAnimationFrame(updateParallax)
    }

    updateParallax()
    window.addEventListener('scroll', onScroll, { passive: true })
    window.addEventListener('resize', onScroll)

    return () => {
      if (frame !== 0) {
        window.cancelAnimationFrame(frame)
      }
      window.removeEventListener('scroll', onScroll)
      window.removeEventListener('resize', onScroll)
    }
  }, [])

  return (
    <div ref={containerRef} aria-hidden className={cn('clawgic-hero-parallax', className)}>
      <Image
        src="/branding/clawgic-philosopher.png"
        alt=""
        fill
        priority
        sizes="100vw"
        className="clawgic-hero-parallax-image"
      />
    </div>
  )
}
