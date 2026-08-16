import { useTranslation } from "react-i18next";
import {
  isExerciseResultFormat,
  type Assignee,
  type HomeworkFormat,
} from "@/lib/admin";
import { StudentLink } from "@/components/admin/students/StudentLink";
import { NotificationDot } from "@/components/NotificationDot";
import { Button } from "@/components/ui/button";

interface Props {
  assignees: Assignee[];
  format: HomeworkFormat;
  onOpenResult: (submissionId: string) => void;
  onOpenReview: (submissionId: string) => void;
}

function canOpenSubmission(a: Assignee): boolean {
  return (
    !!a.submissionId &&
    (a.status === "SUBMITTED" ||
      a.status === "REVIEWED" ||
      a.status === "GRADED")
  );
}

function formatDueOn(iso: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(`${iso.slice(0, 10)}T00:00:00`));
}

export function HomeworkAssigneeList({
  assignees,
  format,
  onOpenResult,
  onOpenReview,
}: Props) {
  const { t, i18n } = useTranslation();

  return (
    <ul className="divide-y rounded-md border">
      {assignees.map((a) => (
        <li
          key={a.userId}
          className="flex items-start justify-between gap-2 px-3 py-2 text-sm"
        >
          <span className="inline-flex min-w-0 flex-1 flex-col gap-0.5">
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
            <span
              className={
                a.dueOn
                  ? "text-xs text-muted-foreground"
                  : "text-xs font-semibold text-foreground"
              }
            >
              {a.dueOn
                ? formatDueOn(a.dueOn, i18n.language)
                : t("admin.homework.noDueOn")}
            </span>
          </span>
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
        </li>
      ))}
    </ul>
  );
}
