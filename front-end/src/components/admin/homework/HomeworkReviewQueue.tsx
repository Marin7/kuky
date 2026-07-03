import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getHomeworkReviewQueue,
  studentDisplayName,
  type HomeworkReviewQueueItem,
} from "@/lib/admin";
import { HomeworkReviewDialog } from "./HomeworkReviewDialog";

/** Cross-student queue of Writing submissions awaiting teacher feedback (FR-010). */
export function HomeworkReviewQueue() {
  const { t } = useTranslation();
  const [queue, setQueue] = useState<HomeworkReviewQueueItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [openSubmissionId, setOpenSubmissionId] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    getHomeworkReviewQueue()
      .then(setQueue)
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const handleReviewed = () => {
    setOpenSubmissionId(null);
    load();
  };

  return (
    <div className="mb-8 rounded-lg border p-4">
      <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
        {t("admin.homeworkReview.queueTitle")}{" "}
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
          {t("admin.homeworkReview.queueEmpty")}
        </p>
      ) : (
        <div className="divide-y rounded-lg border">
          {queue.map((item) => (
            <div
              key={item.submissionId}
              className="flex items-center justify-between px-4 py-3 text-sm"
            >
              <div>
                <p className="font-medium">{item.assignmentTitle}</p>
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
                onClick={() => setOpenSubmissionId(item.submissionId)}
                className="shrink-0 text-xs font-medium text-primary hover:underline"
              >
                {t("admin.homeworkReview.reviewAction")}
              </button>
            </div>
          ))}
        </div>
      )}

      {openSubmissionId && (
        <HomeworkReviewDialog
          submissionId={openSubmissionId}
          onClose={() => setOpenSubmissionId(null)}
          onReviewed={handleReviewed}
        />
      )}
    </div>
  );
}
