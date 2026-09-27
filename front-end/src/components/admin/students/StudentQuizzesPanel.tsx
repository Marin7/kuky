import { useTranslation } from "react-i18next";
import type { StudentQuizSummary } from "@/lib/admin";
import { NotificationDot } from "@/components/NotificationDot";

interface Props {
  quizzes: StudentQuizSummary[];
  onOpenAttempt: (quizId: string, attemptId: string) => void;
}

/** Expanded "Pruebas de evaluación" box: the student's quiz attempts. */
export function StudentQuizzesPanel({ quizzes, onOpenAttempt }: Props) {
  const { t } = useTranslation();
  if (quizzes.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("quiz.admin.noStudentQuizzes")}
      </p>
    );
  }
  return (
    <div className="space-y-3">
      {quizzes.map((q) => (
        <div key={q.attemptId} className="rounded-lg border p-3 text-sm">
          <div className="flex items-start justify-between gap-2">
            <div>
              <p className="inline-flex items-center gap-1.5 font-medium">
                {q.title}
                {q.unseen && <NotificationDot label={t("notification.row")} />}
              </p>
              <p className="text-muted-foreground">
                {t(`quiz.status.${q.status}` as never)}
                {q.scorePercent != null ? ` · ${q.scorePercent}%` : ""}
              </p>
            </div>
            <button
              type="button"
              onClick={() => onOpenAttempt(q.quizId, q.attemptId)}
              className="shrink-0 text-xs font-medium text-primary hover:underline"
            >
              {q.status === "GRADED"
                ? t("quiz.admin.view")
                : t("quiz.admin.review")}
            </button>
          </div>
          {q.skills.length > 0 && (
            <div className="mt-2 grid grid-cols-2 gap-2 text-xs">
              {q.skills.map((s) => (
                <div key={s.skill} className="rounded border p-2">
                  <p className="font-medium">{t(`quiz.skills.${s.skill}`)}</p>
                  <p>
                    {s.awaitingTeacher
                      ? t("quiz.skillAwaiting")
                      : `${s.scorePercent}%`}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
