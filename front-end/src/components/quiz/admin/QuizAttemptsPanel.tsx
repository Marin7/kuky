import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { listQuizAttempts, type QuizAttemptListItem } from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { QuizReviewDialog } from "./QuizReviewDialog";
import { NotificationDot } from "@/components/NotificationDot";
import { notifyBadgesChanged } from "@/lib/notifications";

export function QuizAttemptsPanel({ quizId }: { quizId: string }) {
  const { t } = useTranslation();
  const [attempts, setAttempts] = useState<QuizAttemptListItem[]>([]);
  const [openId, setOpenId] = useState<string | null>(null);

  const load = () => {
    listQuizAttempts(quizId)
      .then(setAttempts)
      .catch(() => setAttempts([]));
  };

  useEffect(load, [quizId]);

  return (
    <div className="space-y-3 border-t pt-6">
      <h2 className="text-lg font-semibold">{t("quiz.admin.attemptsTitle")}</h2>
      {attempts.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {t("quiz.admin.noAttempts")}
        </p>
      ) : (
        <ul className="space-y-2">
          {attempts.map((a) => (
            <li
              key={a.id}
              className="flex items-center justify-between rounded-md border p-3 text-sm"
            >
              <span className="inline-flex items-center gap-1.5">
                {a.studentName} · {t(`quiz.status.${a.status}` as never)}
                {a.scorePercent != null ? ` · ${a.scorePercent}%` : ""}
                {a.unseen && <NotificationDot label={t("notification.row")} />}
              </span>
              <Button
                size="sm"
                variant="outline"
                onClick={() => setOpenId(a.id)}
              >
                {a.status === "GRADED"
                  ? t("quiz.admin.view")
                  : t("quiz.admin.review")}
              </Button>
            </li>
          ))}
        </ul>
      )}
      {openId && (
        <QuizReviewDialog
          quizId={quizId}
          attemptId={openId}
          onClose={() => setOpenId(null)}
          onSaved={() => {
            load();
            notifyBadgesChanged();
          }}
        />
      )}
    </div>
  );
}
