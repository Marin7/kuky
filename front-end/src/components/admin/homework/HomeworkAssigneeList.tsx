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

export function HomeworkAssigneeList({
  assignees,
  format,
  onOpenResult,
  onOpenReview,
}: Props) {
  const { t } = useTranslation();

  return (
    <ul className="divide-y rounded-md border">
      {assignees.map((a) => (
        <li
          key={a.userId}
          className="flex items-center justify-between gap-2 px-3 py-2 text-sm"
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
