import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getActivitySubmission,
  saveActivityFeedback,
  studentDisplayName,
  type ApiError,
  type HomeworkSubmissionAdmin,
} from "@/lib/admin";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { RichTextEditor } from "@/components/learning/richtext/RichTextEditor";
import { RichTextViewer } from "@/components/learning/richtext/RichTextViewer";
import type { FormattedText } from "@/components/learning/richtext/types";

interface Props {
  submissionId: string;
  onClose: () => void;
  onReviewed: () => void;
}

export function ActivityReviewDialog({
  submissionId,
  onClose,
  onReviewed,
}: Props) {
  const { t } = useTranslation();
  const [submission, setSubmission] = useState<HomeworkSubmissionAdmin | null>(
    null,
  );
  const [feedback, setFeedback] = useState<FormattedText>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getActivitySubmission(submissionId)
      .then((data) => {
        setSubmission(data);
        setFeedback(data.feedback ?? []);
      })
      .catch(() => setLoadError(t("admin.activities.reviewLoadError")))
      .finally(() => setLoading(false));
  }, [submissionId, t]);

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      const updated = await saveActivityFeedback(submissionId, feedback);
      setSubmission(updated);
      onReviewed();
    } catch (e) {
      const err = e as ApiError;
      if (err.error === "VALIDATION_ERROR") {
        setError(t("admin.homeworkReview.validationError"));
      } else if (err.error === "ALREADY_REVIEWED") {
        setError(t("admin.homeworkReview.alreadyReviewedError"));
      } else if (err.error === "NOT_SUBMITTED") {
        setError(t("admin.homeworkReview.notSubmittedError"));
      } else {
        setError(t("admin.activities.reviewGenericError"));
      }
    } finally {
      setSaving(false);
    }
  };

  const reviewed = submission?.status === "REVIEWED";
  const multiAnswers = submission?.answers?.filter(Boolean) ?? [];
  const hasMultiAnswers = multiAnswers.length > 0;

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[90vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {submission?.assignmentTitle ?? t("admin.activities.reviewTitle")}
          </DialogTitle>
        </DialogHeader>

        {loading ? (
          <p className="animate-pulse text-sm text-muted-foreground">
            {t("common.loading")}
          </p>
        ) : loadError ? (
          <p className="text-sm text-destructive">{loadError}</p>
        ) : submission ? (
          <div className="space-y-4">
            <p className="text-xs text-muted-foreground">
              {studentDisplayName({
                firstName: submission.studentFirstName,
                lastName: submission.studentLastName,
                username: submission.studentUsername,
                email: submission.studentEmail,
              })}
            </p>
            <div>
              <p className="mb-1 text-xs font-medium text-muted-foreground">
                {hasMultiAnswers
                  ? t("admin.homeworkReview.studentAnswers")
                  : t("admin.homeworkReview.studentAnswer")}
              </p>
              {hasMultiAnswers ? (
                <ul className="space-y-3">
                  {multiAnswers.map((a, i) => (
                    <li
                      key={a.questionId ?? `ans-${i}`}
                      className="rounded-md border bg-muted/20 p-3"
                    >
                      <p className="mb-1 text-xs font-medium text-muted-foreground">
                        {t("admin.homeworkReview.questionPrompt", {
                          index: i + 1,
                        })}
                      </p>
                      <p className="mb-2 text-sm font-medium whitespace-pre-wrap">
                        {a.promptSnapshot ||
                          t("admin.homeworkReview.deletedPrompt")}
                      </p>
                      <p className="text-xs font-medium text-muted-foreground">
                        {t("admin.homeworkReview.answerLabel")}
                      </p>
                      <p className="text-sm whitespace-pre-wrap">
                        {a.text || t("admin.homeworkReview.emptyAnswer")}
                      </p>
                    </li>
                  ))}
                </ul>
              ) : (
                <div className="rounded-md border bg-muted/20 p-3">
                  <RichTextViewer segments={submission.response ?? []} />
                </div>
              )}
            </div>
            <div>
              <p className="mb-1 text-xs font-medium text-muted-foreground">
                {t("admin.homeworkReview.yourFeedback")}
              </p>
              {reviewed ? (
                <div className="rounded-md border bg-muted/20 p-3">
                  <RichTextViewer segments={feedback} />
                </div>
              ) : (
                <RichTextEditor
                  value={feedback}
                  onChange={setFeedback}
                  placeholder={t("admin.homeworkReview.feedbackPlaceholder")}
                  disabled={saving}
                />
              )}
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
            {!reviewed && (
              <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={onClose}>
                  {t("admin.homeworkReview.close")}
                </Button>
                <Button type="button" disabled={saving} onClick={handleSave}>
                  {saving
                    ? t("admin.homeworkReview.saving")
                    : t("admin.homeworkReview.save")}
                </Button>
              </div>
            )}
          </div>
        ) : null}
      </DialogContent>
    </Dialog>
  );
}
