import { cn } from '@/lib/utils'

type ClawgicLogoProps = {
  className?: string
  labelClassName?: string
  showWordmark?: boolean
}

export function ClawgicLogo({
  className,
  labelClassName,
  showWordmark = true,
}: ClawgicLogoProps) {
  if (!showWordmark) {
    return null
  }

  return (
    <span className={cn('inline-flex items-center', className)}>
      <span
        className={cn('text-lg font-semibold tracking-tight text-foreground', labelClassName)}
        data-testid="clawgic-logo-wordmark"
      >
        Clawgic
      </span>
    </span>
  )
}
