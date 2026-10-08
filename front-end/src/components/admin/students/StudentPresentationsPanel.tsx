import { useState } from "react";
import { useTranslation } from "react-i18next";
import type {
  StudentProfilePresentation,
  StudentProfileUnitActivity,
} from "@/lib/admin";
import { NotificationDot } from "@/components/NotificationDot";
import { ActivitySubmissionDialog } from "@/components/admin/units/ActivitySubmissionDialog";

interface Props {
  presentations: StudentProfilePresentation[];
  unitActivities: StudentProfileUnitActivity[];
  /** Reload the profile after a review (status / unseen changed). */
  onActivityChanged: () => void;
}

/** Expanded "Presentaciones" box: presentations the student can see, then their unit activities. */
export function StudentPresentationsPanel({
  presentations,
  unitActivities,
  onActivityChanged,
}: Props) {
  const { t } = useTranslation();
  const [open, setOpen] = useState<StudentProfileUnitActivity | null>(null);

  if (presentations.length === 0 && unitActivities.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("admin.studentProfile.emptyPresentations")}
      </p>
    );
  }

  const byUnit = new Map<string, StudentProfileUnitActivity[]>();
  for (const a of unitActivities) {
    byUnit.set(a.unitId, [...(byUnit.get(a.unitId) ?? []), a]);
  }

  return (
    <div className="space-y-4">
      {presentations.length > 0 && (
        <div className="divide-y rounded-lg border">
          {presentations.map((p) => (
            <div
              key={p.id}
              className="flex items-center justify-between px-4 py-3 text-sm"
            >
              <span>{p.title}</span>
              {p.level && (
                <span className="ml-4 shrink-0 rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                  {p.level}
                </span>
              )}
            </div>
          ))}
        </div>
      )}

      {[...byUnit.values()].map((activities) => (
        <div key={activities[0].unitId} className="space-y-1.5">
          <p className="text-sm font-medium">
            {t("admin.studentProfile.unitActivities", {
              level: activities[0].unitLevel,
              subject: activities[0].unitSubject,
            })}
          </p>
          <div className="divide-y rounded-lg border">
            {activities.map((a) => {
              const title =
                a.title ?? t("learning.activities.pageTitle", { page: a.page });
              const label = (
                <>
                  <span className="min-w-0 truncate">
                    <span className="mr-2 text-muted-foreground">
                      {t("learning.activities.pageShort", { page: a.page })}
                    </span>
                    {title}
                  </span>
                  <span className="inline-flex shrink-0 items-center gap-1.5">
                    {a.unseen && (
                      <NotificationDot label={t("notification.activity")} />
                    )}
                    <span className="rounded-full bg-muted px-2 py-0.5 text-xs">
                      {t(`learning.homework.status.${a.status}`)}
                      {a.status === "GRADED" &&
                        a.scorePercent != null &&
                        ` — ${a.scorePercent}%`}
                    </span>
                  </span>
                </>
              );
              return a.submissionId ? (
                <button
                  key={a.activityId}
                  type="button"
                  onClick={() => setOpen(a)}
                  className="flex w-full items-center justify-between gap-2 px-4 py-2.5 text-left text-sm hover:bg-muted/40"
                >
                  {label}
                </button>
              ) : (
                <div
                  key={a.activityId}
                  className="flex items-center justify-between gap-2 px-4 py-2.5 text-sm"
                >
                  {label}
                </div>
              );
            })}
          </div>
        </div>
      ))}

      {open?.submissionId && (
        <ActivitySubmissionDialog
          submissionId={open.submissionId}
          format={open.format}
          onClose={() => setOpen(null)}
          onChanged={onActivityChanged}
        />
      )}
    </div>
  );
}
