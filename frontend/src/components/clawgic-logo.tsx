import { cn } from '@/lib/utils'

type ClawgicLogoProps = {
  className?: string
  labelClassName?: string
  markClassName?: string
  showWordmark?: boolean
}

export function ClawgicLogo({
  className,
  labelClassName,
  markClassName,
  showWordmark = true,
}: ClawgicLogoProps) {
  return (
    <span className={cn('inline-flex items-center gap-2', className)}>
      <span
        className={cn(
          'clawgic-logo-mark inline-flex h-8 w-8 items-center justify-center rounded-xl',
          markClassName
        )}
        aria-hidden
        data-testid="clawgic-logo-mark"
      >
        <svg viewBox="0 0 32 32" className="h-5 w-5" fill="none" role="presentation">
          <circle cx="11" cy="11" r="4.5" className="fill-current text-white/95" />
          <circle cx="21" cy="11" r="4.5" className="fill-current text-white/95" />
          <path
            d="M8 20.5c2.7-1.3 4.9-0.7 8 1.2c3.1-1.9 5.3-2.5 8-1.2c-0.7 3.7-3.8 6.2-8 6.2s-7.3-2.5-8-6.2z"
            className="fill-current text-white"
          />
        </svg>
      </span>
      {showWordmark ? (
        <span className={cn('text-lg font-semibold tracking-tight text-foreground', labelClassName)}>
          Clawgic
        </span>
      ) : null}
    </span>
  )
}
