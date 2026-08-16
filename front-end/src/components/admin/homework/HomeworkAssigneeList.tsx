import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  isExerciseResultFormat,
  updateAssigneeDueOn,
  localIsoDate,
  type Assignee,
  type HomeworkAdminItem,
  type HomeworkFormat,
} from "@/lib/admin";
import { StudentLink } from "@/components/admin/students/StudentLink";
import { NotificationDot } from "@/components/NotificationDot";
import { Button } from "@/components/ui/button";
import { DueOnDateInput } from "./DueOnDateInput";

interface Props {
  assignees: Assignee[];
  format: HomeworkFormat;
  homeworkId?: string;
  onOpenResult: (submissionId: string) => void;
  onOpenReview: (submissionId: string) => void;
  onUpdated?: (item: HomeworkAdminItem) => void;
}

function canOpenSubmission(a: Assignee): boolean {
  return (
    !!a.submissionId &&
    (a.status === "SUBMITTED" ||
      a.status === "REVIEWED" ||
      a.status === "GRADED")
  );
}

export function HomeworkAssigneeList({
  assignees,
  format,
  homeworkId,
  onOpenResult,
  onOpenReview,
  onUpdated,
}: Props) {
  const { t } = useTranslation();
  const [savingUserId, setSavingUserId] = useState<string | null>(null);

  const persistDueOn = async (userId: string, dueOn: string | null) => {
    if (!homeworkId || !onUpdated) return;
    if (dueOn && dueOn < localIsoDate()) return;
    setSavingUserId(userId);
    try {
      const updated = await updateAssigneeDueOn(homeworkId, userId, dueOn);
      onUpdated(updated);
    } finally {
      setSavingUserId(null);
    }
  };

  return (
    <ul className="divide-y rounded-md border">
      {assignees.map((a) => (
        <li
          key={a.userId}
          className="flex flex-col gap-2 px-3 py-2 text-sm sm:flex-row sm:items-start sm:justify-between"
        >
          <span className="inline-flex min-w-0 items-center gap-1.5">
            <StudentLink
              student={{
                id: a.userId,
                email: a.email,
                firstName: a.firstName,
                lastName: a.lastName,
                username: a.username,
              }}
            />
            {a.unseen && <NotificationDot label={t("notification.row")} />}
            {a.overdue && (
              <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-800">
                {t("learning.homework.overdue")}
              </span>
            )}
          </span>
          <span className="flex shrink-0 items-center gap-2">
            {homeworkId && onUpdated ? (
              <DueOnDateInput
                value={a.dueOn ?? ""}
                disabled={savingUserId === a.userId}
                onChange={(next) => void persistDueOn(a.userId, next || null)}
              />
            ) : a.dueOn ? (
              <span className="text-xs text-muted-foreground">
                {t("admin.homework.dueOn")} {a.dueOn}
              </span>
            ) : (
              <span className="text-xs font-semibold text-foreground">
                {t("admin.homework.noDueOn")}
              </span>
            )}
            {canOpenSubmission(a) && (
              <Button
                type="button"
                size="sm"
                variant="outline"
                className="h-7 shrink-0 text-xs"
                onClick={() => {
                  const id = a.submissionId;
                  if (!id) return;
                  if (a.status === "GRADED" && isExerciseResultFormat(format)) {
                    onOpenResult(id);
                  } else {
                    onOpenReview(id);
                  }
                }}
              >
                {a.status === "GRADED"
                  ? t("admin.exerciseResult.viewAction")
                  : t("admin.homeworkReview.reviewAction")}
              </Button>
            )}
          </span>
        </li>
      ))}
    </ul>
  );
}
