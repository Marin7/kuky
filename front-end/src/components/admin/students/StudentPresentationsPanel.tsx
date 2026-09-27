import { useTranslation } from "react-i18next";
import type { StudentProfilePresentation } from "@/lib/admin";

interface Props {
  presentations: StudentProfilePresentation[];
}

/** Expanded "Presentaciones" box: presentations shared with the student. */
export function StudentPresentationsPanel({ presentations }: Props) {
  const { t } = useTranslation();
  if (presentations.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("admin.studentProfile.emptyPresentations")}
      </p>
    );
  }
  return (
    <div className="divide-y rounded-lg border">
      {presentations.map((p) => (
        <div
          key={p.id}
          className="flex items-center justify-between px-4 py-3 text-sm"
        >
          <span>{p.title}</span>
          {p.level && (
            <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground ml-4 shrink-0">
              {p.level}
            </span>
          )}
        </div>
      ))}
    </div>
  );
}
