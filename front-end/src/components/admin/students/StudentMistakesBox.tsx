import { useTranslation } from "react-i18next";

interface Props {
  /** All-time mistake count; `null` while loading or when it failed to load. */
  count: number | null;
  expanded: boolean;
  onToggle: () => void;
}

/** "Errores frecuentes" stat card on the admin student profile — toggles its list. */
export function StudentMistakesBox({ count, expanded, onToggle }: Props) {
  const { t } = useTranslation();
  return (
    <button
      type="button"
      aria-expanded={expanded}
      onClick={onToggle}
      className={`rounded-lg border bg-card p-4 text-center transition-colors hover:bg-muted/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ${
        expanded ? "ring-2 ring-primary/30" : ""
      }`}
    >
      <p className="text-2xl font-semibold">{count ?? "…"}</p>
      <p className="text-xs text-muted-foreground mt-1">
        {t("admin.studentProfile.mistakes.title")}
      </p>
      <p className="mt-2 text-[11px] text-muted-foreground">
        {expanded
          ? t("admin.studentProfile.mistakes.collapse")
          : t("admin.studentProfile.mistakes.expand")}
      </p>
    </button>
  );
}
