import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getQuizReviewQueue,
  studentDisplayName,
  type QuizReviewQueueItem,
} from "@/lib/admin";
import { QuizReviewDialog } from "./QuizReviewDialog";
import { NotificationDot } from "@/components/NotificationDot";
import { notifyBadgesChanged, onBadgesInvalidate } from "@/lib/notifications";

export function QuizReviewQueue() {
  const { t } = useTranslation();
  const [queue, setQueue] = useState<QuizReviewQueueItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState<QuizReviewQueueItem | null>(null);

  const load = () => {
    setLoading(true);
    getQuizReviewQueue()
      .then(setQueue)
      .catch(() => setQueue([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    return onBadgesInvalidate(() => {
      getQuizReviewQueue()
        .then(setQueue)
        .catch(() => setQueue([]));
    });
  }, []);

  return (
    <div className="mb-8 rounded-lg border p-4">
      <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
        {t("quiz.admin.queueTitle")}{" "}
        <span className="ml-1 rounded-full bg-muted px-2 py-0.5 text-xs font-normal">
          {queue.length}
        </span>
      </h2>

      {loading ? (
        <p className="animate-pulse text-sm text-muted-foreground">
          {t("common.loading")}
        </p>
      ) : queue.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {t("quiz.admin.queueEmpty")}
        </p>
      ) : (
        <div className="divide-y rounded-lg border">
          {queue.map((item) => (
            <div
              key={item.attemptId}
              className="flex items-center justify-between px-4 py-3 text-sm"
            >
              <div>
                <p className="inline-flex items-center gap-1.5 font-medium">
                  {item.quizTitle}
                  {item.unseen && (
                    <NotificationDot label={t("notification.row")} />
                  )}
                </p>
                <p className="text-xs text-muted-foreground">
                  {studentDisplayName({
                    firstName: item.studentFirstName,
                    lastName: item.studentLastName,
                    username: item.studentUsername,
                    email: item.studentEmail,
                  })}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setOpen(item)}
                className="shrink-0 text-xs font-medium text-primary hover:underline"
              >
                {t("quiz.admin.review")}
              </button>
            </div>
          ))}
        </div>
      )}

      {open && (
        <QuizReviewDialog
          quizId={open.quizId}
          attemptId={open.attemptId}
          onClose={() => setOpen(null)}
          onSaved={() => {
            load();
            notifyBadgesChanged();
          }}
        />
      )}
    </div>
  );
}
