import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getHomeworkSubmission,
  saveHomeworkFeedback,
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

/**
 * Review screen for a single Writing submission: shows the student's
 * formatted answer read-only, and lets the teacher write formatted feedback
 * (color/highlight/strike) and save it, which transitions the submission to
 * REVIEWED. Once REVIEWED, both sides render read-only.
 */
export function HomeworkReviewDialog({
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
    getHomeworkSubmission(submissionId)
      .then((data) => {
        setSubmission(data);
        setFeedback(data.feedback ?? []);
      })
      .catch(() => setLoadError(t("admin.homeworkReview.loadError")))
      .finally(() => setLoading(false));
  }, [submissionId, t]);

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      const updated = await saveHomeworkFeedback(submissionId, feedback);
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
        setError(t("admin.homeworkReview.genericError"));
      }
    } finally {
      setSaving(false);
    }
  };

  const readOnly = submission?.status === "REVIEWED";

  return (
    <Dialog open onOpenChange={(next) => !next && onClose()}>
      <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {submission
              ? submission.assignmentTitle
              : t("admin.homeworkReview.dialogTitle")}
          </DialogTitle>
        </DialogHeader>

        {loading && (
          <p className="animate-pulse text-sm text-muted-foreground">
            {t("common.loading")}
          </p>
        )}
        {loadError && <p className="text-sm text-destructive">{loadError}</p>}

        {submission && (
          <div className="space-y-6">
            <p className="text-xs text-muted-foreground">
              {studentDisplayName({
                firstName: submission.studentFirstName,
                lastName: submission.studentLastName,
                username: submission.studentUsername,
                email: submission.studentEmail,
              })}
            </p>

            <div>
              <p className="mb-2 text-sm font-medium">
                {t("admin.homeworkReview.studentAnswer")}
              </p>
              <div className="rounded-md border bg-muted/20 p-3">
                <RichTextViewer segments={submission.response} />
              </div>
            </div>

            <div>
              <p className="mb-2 text-sm font-medium">
                {t("admin.homeworkReview.yourFeedback")}
              </p>
              {readOnly ? (
                <div className="rounded-md border bg-muted/20 p-3">
                  <RichTextViewer segments={submission.feedback ?? []} />
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

            {!readOnly && (
              <div className="flex justify-end gap-3">
                <Button variant="outline" onClick={onClose} disabled={saving}>
                  {t("admin.homeworkReview.close")}
                </Button>
                <Button onClick={handleSave} disabled={saving}>
                  {saving
                    ? t("admin.homeworkReview.saving")
                    : t("admin.homeworkReview.save")}
                </Button>
              </div>
            )}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
