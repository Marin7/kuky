import { cn } from "@/lib/utils";

/** Presence-only unseen mark. Never shows a count. */
export function NotificationDot({
  label,
  className,
}: {
  label: string;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-block h-2 w-2 shrink-0 rounded-full bg-primary",
        className,
      )}
      role="img"
      aria-label={label}
    />
  );
}
