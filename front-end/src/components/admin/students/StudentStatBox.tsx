import { useTranslation } from "react-i18next";
import { NotificationDot } from "@/components/NotificationDot";

interface Props {
  title: string;
  /** `null` while loading or when it failed to load. */
  count: number | null;
  /** Optional per-status counts, rendered one per line under the title. */
  breakdown?: { label: string; value: number }[];
  unseen?: boolean;
  expanded: boolean;
  onToggle: () => void;
  expandLabel: string;
  collapseLabel: string;
}

/** Stat card on the admin student profile — toggles its section's detail panel. */
export function StudentStatBox({
  title,
  count,
  breakdown,
  unseen = false,
  expanded,
  onToggle,
  expandLabel,
  collapseLabel,
}: Props) {
  const { t } = useTranslation();
  return (
    <button
      type="button"
      aria-expanded={expanded}
      onClick={onToggle}
      className={`flex flex-col items-center rounded-lg border bg-card p-3 text-center transition-colors hover:bg-muted/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ${
        expanded ? "ring-2 ring-primary/30" : ""
      }`}
    >
      <p className="text-2xl font-semibold">{count ?? "…"}</p>
      <p className="mt-1 inline-flex items-center gap-1.5 text-xs text-muted-foreground">
        {title}
        {unseen && <NotificationDot label={t("notification.row")} />}
      </p>
      {breakdown && (
        <div className="mt-2 space-y-0.5">
          {breakdown.map((line) => (
            <p key={line.label} className="text-[11px] leading-tight">
              <span className="font-semibold">{line.value}</span>{" "}
              <span className="text-muted-foreground">{line.label}</span>
            </p>
          ))}
        </div>
      )}
      <p className="mt-auto pt-2 text-[11px] text-muted-foreground">
        {expanded ? collapseLabel : expandLabel}
      </p>
    </button>
  );
}
