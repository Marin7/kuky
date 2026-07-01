import { useTranslation } from "react-i18next";

interface StudentOnlyNoticeProps {
  className?: string;
}

/** Shown wherever a student-only action or page is blocked for a logged-in
 * user who hasn't been granted student status yet. */
export function StudentOnlyNotice({ className }: StudentOnlyNoticeProps) {
  const { t } = useTranslation();

  return (
    <div
      className={`rounded-xl border bg-muted/40 p-6 text-center space-y-1 ${className ?? ""}`}
    >
      <p className="font-medium">{t("studentOnly.title")}</p>
      <p className="text-sm text-muted-foreground">
        {t("studentOnly.message")}
      </p>
    </div>
  );
}
