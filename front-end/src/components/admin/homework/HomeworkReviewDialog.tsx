import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getHomeworkSubmission,
  saveHomeworkFeedback,
  studentDisplayName,
  type ApiError,
  type HomeworkSubmissionAdmin,
  type SaveHomeworkReviewPayload,
} from "@/lib/admin";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { RichTextEditor } from "@/components/learning/richtext/RichTextEditor";
import { RichTextViewer } from "@/components/learning/richtext/RichTextViewer";
import {
  plainText,
  type FormattedText,
} from "@/components/learning/richtext/types";

const MAX_FEEDBACK = 500;

interface Props {
  submissionId: string;
  onClose: () => void;
  onReviewed: () => void;
}

function toFormatted(
  text: string,
  formatted?: FormattedText | null,
): FormattedText {
  if (formatted && formatted.length > 0) return formatted;
  if (!text) return [];
  return [{ text }];
}

function isEditableReview(s: HomeworkSubmissionAdmin): boolean {
  return (
    s.status === "SUBMITTED" ||
    (s.status === "REVIEWED" && s.reviewModel === "ANNOTATED")
  );
}

/**
 * MANUAL homework review: annotate student answers in place + optional plain ≤500 note.
 * LEGACY_RICH reviews stay view-only with rich feedback.
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
  const [response, setResponse] = useState<FormattedText>([]);
  const [answerFormats, setAnswerFormats] = useState<
    Record<string, FormattedText>
  >({});
  const [feedbackText, setFeedbackText] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const hydrate = (data: HomeworkSubmissionAdmin) => {
    setSubmission(data);
    setResponse(data.response ?? []);
    const map: Record<string, FormattedText> = {};
    for (const a of data.answers ?? []) {
      const key = a.questionId ?? `null:${a.promptSnapshot}`;
      map[key] = toFormatted(a.text, a.formatted);
    }
    setAnswerFormats(map);
    setFeedbackText(data.feedbackText ?? "");
  };

  useEffect(() => {
    getHomeworkSubmission(submissionId)
      .then(hydrate)
      .catch(() => setLoadError(t("admin.homeworkReview.loadError")))
      .finally(() => setLoading(false));
  }, [submissionId, t]);

  const multiAnswers = submission?.answers?.filter(Boolean) ?? [];
  const hasMultiAnswers = multiAnswers.length > 0;
  const legacy = submission?.reviewModel === "LEGACY_RICH";
  const editable = submission ? isEditableReview(submission) : false;

  const handleSave = async () => {
    if (!submission || !editable) return;
    const trimmed = feedbackText.trim();
    if (trimmed.length > MAX_FEEDBACK) {
      setError(t("admin.homeworkReview.feedbackTooLong"));
      return;
    }

    const payload: SaveHomeworkReviewPayload = {
      feedbackText: trimmed,
    };
    if (hasMultiAnswers) {
      payload.answers = multiAnswers.map((a) => {
        const key = a.questionId ?? `null:${a.promptSnapshot}`;
        const formatted = answerFormats[key] ?? toFormatted(a.text, a.formatted);
        return {
          questionId: a.questionId,
          formatted:
            formatted.length > 0 ? formatted : [{ text: a.text || "" }],
        };
      });
    } else {
      payload.response =
        response.length > 0
          ? response
          : [{ text: plainText(submission.response ?? []) || " " }];
    }

    setSaving(true);
    setError(null);
    try {
      const updated = await saveHomeworkFeedback(submissionId, payload);
      hydrate(updated);
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

            {legacy && (
              <p className="text-sm text-muted-foreground">
                {t("admin.homeworkReview.legacyFrozen")}
              </p>
            )}

            <div>
              <p className="mb-2 text-sm font-medium">
                {hasMultiAnswers
                  ? t("admin.homeworkReview.studentAnswers")
                  : t("admin.homeworkReview.studentAnswer")}
              </p>
              {hasMultiAnswers ? (
                <ul className="space-y-3">
                  {multiAnswers.map((a, i) => {
                    const key = a.questionId ?? `null:${a.promptSnapshot}`;
                    const formatted =
                      answerFormats[key] ?? toFormatted(a.text, a.formatted);
                    return (
                      <li
                        key={key}
                        className="rounded-md border bg-muted/20 p-3"
                      >
                        <p className="mb-1 text-xs font-medium text-muted-foreground">
                          {t("admin.homeworkReview.questionPrompt", {
                            index: i + 1,
                          })}
                        </p>
                        <p className="mb-2 text-sm font-medium whitespace-pre-wrap break-all [overflow-wrap:anywhere]">
                          {a.promptSnapshot ||
                            t("admin.homeworkReview.deletedPrompt")}
                        </p>
                        <p className="mb-1 text-xs font-medium text-muted-foreground">
                          {t("admin.homeworkReview.answerLabel")}
                        </p>
                        {editable ? (
                          <RichTextEditor
                            value={formatted}
                            onChange={(next) =>
                              setAnswerFormats((prev) => ({
                                ...prev,
                                [key]: next,
                              }))
                            }
                            formatOnly
                            rows={4}
                            disabled={saving}
                          />
                        ) : (
                          <div className="text-sm break-all [overflow-wrap:anywhere]">
                            {formatted.length > 0 ? (
                              <RichTextViewer segments={formatted} />
                            ) : (
                              <p className="whitespace-pre-wrap">
                                {a.text ||
                                  t("admin.homeworkReview.emptyAnswer")}
                              </p>
                            )}
                          </div>
                        )}
                      </li>
                    );
                  })}
                </ul>
              ) : editable ? (
                <RichTextEditor
                  value={response}
                  onChange={setResponse}
                  formatOnly
                  rows={10}
                  disabled={saving}
                />
              ) : (
                <div className="rounded-md border bg-muted/20 p-3">
                  <RichTextViewer segments={submission.response ?? []} />
                </div>
              )}
            </div>

            <div>
              <p className="mb-2 text-sm font-medium">
                {t("admin.homeworkReview.yourFeedback")}
              </p>
              {legacy ? (
                <div className="rounded-md border bg-muted/20 p-3">
                  <RichTextViewer segments={submission.feedback ?? []} />
                </div>
              ) : editable ? (
                <div className="space-y-1">
                  <Textarea
                    value={feedbackText}
                    onChange={(e) => setFeedbackText(e.target.value)}
                    placeholder={t("admin.homeworkReview.feedbackPlaceholder")}
                    rows={3}
                    maxLength={MAX_FEEDBACK}
                    disabled={saving}
                    className="break-all [overflow-wrap:anywhere]"
                  />
                  <p className="text-right text-xs text-muted-foreground tabular-nums">
                    {feedbackText.trim().length} / {MAX_FEEDBACK}
                  </p>
                </div>
              ) : submission.feedbackText ? (
                <div className="rounded-md border bg-muted/20 p-3 text-sm whitespace-pre-wrap break-all [overflow-wrap:anywhere]">
                  {submission.feedbackText}
                </div>
              ) : null}
            </div>

            {error && <p className="text-sm text-destructive">{error}</p>}

            {editable && (
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
