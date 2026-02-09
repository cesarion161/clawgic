import { cn } from '@/lib/utils'

interface VerifiedBadgeProps {
  twitterUsername?: string
  className?: string
}

export function VerifiedBadge({ twitterUsername, className }: VerifiedBadgeProps) {
  if (!twitterUsername) return null

  return (
    <div
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full bg-blue-500/10 px-2.5 py-1 text-xs font-medium text-blue-500',
        className
      )}
    >
      <svg
        className="h-3.5 w-3.5"
        fill="currentColor"
        viewBox="0 0 20 20"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          fillRule="evenodd"
          d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z"
          clipRule="evenodd"
        />
      </svg>
      <span>@{twitterUsername}</span>
    </div>
  )
}
