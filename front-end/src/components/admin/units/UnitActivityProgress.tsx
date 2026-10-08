import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getUnitActivityProgress,
  studentDisplayName,
  type UnitActivityProgressRow,
  type UnitDetail,
} from "@/lib/admin";
import { NotificationDot } from "@/components/NotificationDot";
import { ActivitySubmissionDialog } from "./ActivitySubmissionDialog";

interface Props {
  unit: UnitDetail;
  /** Bumped by the parent when activities change, to reload the grid. */
  reloadKey: number;
  /** After a review: refresh the unit (counts, dots). */
  onChanged: () => void;
}

const STATUS_CLASS: Record<UnitActivityProgressRow["status"], string> = {
  PENDING: "bg-muted text-muted-foreground",
  SUBMITTED: "bg-amber-100 text-amber-800",
  REVIEWED: "bg-blue-100 text-blue-700",
  GRADED: "bg-green-100 text-green-700",
};

/** Per activity, each assigned student's status; a submitted row opens its result / review. */
export function UnitActivityProgress({ unit, reloadKey, onChanged }: Props) {
  const { t } = useTranslation();
  const [rows, setRows] = useState<UnitActivityProgressRow[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState<UnitActivityProgressRow | null>(null);

  const load = () => {
    getUnitActivityProgress(unit.id)
      .then((r) => {
        setRows(r);
        setError(null);
      })
      .catch(() => setError(t("admin.units.editor.progressLoadError")));
  };

  // eslint-disable-next-line react-hooks/exhaustive-deps -- reload on unit / parent bump
  useEffect(load, [unit.id, reloadKey]);

  if (unit.activities.length === 0) return null;
  if (error) return <p className="text-sm text-destructive">{error}</p>;
  if (!rows) {
    return (
      <p className="animate-pulse text-sm text-muted-foreground">
        {t("admin.units.loading")}
      </p>
    );
  }
  if (unit.assignedStudents.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("admin.units.editor.progressNoStudents")}
      </p>
    );
  }

  const studentsById = new Map(unit.assignedStudents.map((s) => [s.id, s]));
  const formatById = new Map(unit.activities.map((a) => [a.id, a.format]));

  return (
    <div className="space-y-3">
      {unit.activities.map((activity) => {
        const activityRows = rows.filter((r) => r.activityId === activity.id);
        return (
          <div key={activity.id} className="space-y-1">
            <p className="text-sm font-medium">
              <span className="mr-2 text-muted-foreground">
                {t("learning.activities.pageShort", { page: activity.page })}
              </span>
              {activity.title ??
                t("learning.activities.pageTitle", { page: activity.page })}
            </p>
            <div className="divide-y rounded-md border">
              {activityRows.map((r) => {
                const student = studentsById.get(r.studentId);
                const content = (
                  <>
                    <span className="min-w-0 truncate">
                      {student ? studentDisplayName(student) : r.studentId}
                    </span>
                    <span className="inline-flex shrink-0 items-center gap-1.5">
                      {r.unseen && (
                        <NotificationDot label={t("notification.activity")} />
                      )}
                      <span
                        className={[
                          "rounded-full px-2 py-0.5 text-xs font-medium",
                          STATUS_CLASS[r.status],
                        ].join(" ")}
                      >
                        {r.status === "SUBMITTED"
                          ? t("admin.units.editor.awaitingCorrection")
                          : t(`learning.homework.status.${r.status}`)}
                        {r.status === "GRADED" &&
                          r.scorePercent != null &&
                          ` — ${r.scorePercent}%`}
                      </span>
                    </span>
                  </>
                );
                return r.submissionId ? (
                  <button
                    key={r.studentId}
                    type="button"
                    onClick={() => setOpen(r)}
                    className="flex w-full items-center justify-between gap-2 px-3 py-2 text-left text-sm hover:bg-muted/40"
                  >
                    {content}
                  </button>
                ) : (
                  <div
                    key={r.studentId}
                    className="flex items-center justify-between gap-2 px-3 py-2 text-sm"
                  >
                    {content}
                  </div>
                );
              })}
            </div>
          </div>
        );
      })}

      {open?.submissionId && (
        <ActivitySubmissionDialog
          submissionId={open.submissionId}
          format={formatById.get(open.activityId) ?? "MANUAL"}
          onClose={() => setOpen(null)}
          onChanged={() => {
            load();
            onChanged();
          }}
        />
      )}
    </div>
  );
}
