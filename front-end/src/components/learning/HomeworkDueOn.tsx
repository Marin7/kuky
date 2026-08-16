import { useTranslation } from "react-i18next";

function formatHomeworkDueOn(iso: string, locale = "es"): string {
  const day = iso.slice(0, 10);
  return new Intl.DateTimeFormat(locale, {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(`${day}T00:00:00`));
}

/** Student-facing due date. Renders nothing when the student has no deadline. */
export function HomeworkDueOn({
  dueOn,
  className = "text-sm text-muted-foreground",
}: {
  dueOn: string | null | undefined;
  className?: string;
}) {
  const { t, i18n } = useTranslation();
  if (!dueOn) return null;
  return (
    <span className={className}>
      {t("learning.homework.dueOn")}{" "}
      <span className="capitalize">
        {formatHomeworkDueOn(dueOn, i18n.language)}
      </span>
    </span>
  );
}
