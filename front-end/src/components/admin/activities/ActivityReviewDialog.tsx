import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getActivitySubmission,
  saveActivityFeedback,
  studentDisplayName,
  type ApiError,
  type HomeworkSubmissionAdmin,
  type SaveHomeworkReviewPayload,
  type TeacherValidation,
} from "@/lib/admin";
import { resolveComposition } from "@/lib/learning";
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
import { ExerciseResult } from "@/components/learning/ExerciseResult";
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
  const composition = resolveComposition(s);
  const scoredManual =
    composition === "MIXED" || composition === "ALL_MANUAL";
  return (
    s.status === "SUBMITTED" ||
    (s.status === "REVIEWED" && s.reviewModel === "ANNOTATED") ||
    (s.status === "GRADED" &&
      scoredManual &&
      s.reviewModel === "ANNOTATED")
  );
}

function isFreeTextAnswer(a: {
  kind?: string | null;
  text?: string;
}): boolean {
  return !a.kind || a.kind === "FREE_TEXT";
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
  const [response, setResponse] = useState<FormattedText>([]);
  const [answerFormats, setAnswerFormats] = useState<
    Record<string, FormattedText>
  >({});
  const [validations, setValidations] = useState<
    Record<string, TeacherValidation | null>
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
    const vals: Record<string, TeacherValidation | null> = {};
    for (const a of data.answers ?? []) {
      const key = a.questionId ?? `null:${a.promptSnapshot}`;
      map[key] = toFormatted(a.text, a.formatted);
      vals[key] = a.teacherValidation ?? null;
    }
    setAnswerFormats(map);
    setValidations(vals);
    setFeedbackText(data.feedbackText ?? "");
  };

  useEffect(() => {
    getActivitySubmission(submissionId)
      .then(hydrate)
      .catch(() => setLoadError(t("admin.activities.reviewLoadError")))
      .finally(() => setLoading(false));
  }, [submissionId, t]);

  const multiAnswers = submission?.answers?.filter(Boolean) ?? [];
  const freeTextAnswers = multiAnswers.filter(isFreeTextAnswer);
  const hasMultiAnswers = freeTextAnswers.length > 0;
  const composition = submission ? resolveComposition(submission) : null;
  const isMixed = composition === "MIXED";
  const needsValidation =
    composition === "MIXED" || composition === "ALL_MANUAL";
  const legacy = submission?.reviewModel === "LEGACY_RICH";
  const editable = submission ? isEditableReview(submission) : false;
  const allValidated =
    !needsValidation ||
    freeTextAnswers.every((a) => {
      const key = a.questionId ?? `null:${a.promptSnapshot}`;
      const v = validations[key];
      return v === "VALIDATED" || v === "INVALIDATED";
    });

  const handleSave = async () => {
    if (!submission || !editable) return;
    const trimmed = feedbackText.trim();
    if (trimmed.length > MAX_FEEDBACK) {
      setError(t("admin.homeworkReview.feedbackTooLong"));
      return;
    }
    if (needsValidation && !allValidated) {
      setError(t("admin.homeworkReview.validationRequired"));
      return;
    }

    const payload: SaveHomeworkReviewPayload = {
      feedbackText: trimmed,
    };
    if (hasMultiAnswers) {
      payload.answers = freeTextAnswers.map((a) => {
        const key = a.questionId ?? `null:${a.promptSnapshot}`;
        const formatted = answerFormats[key] ?? toFormatted(a.text, a.formatted);
        const item: NonNullable<SaveHomeworkReviewPayload["answers"]>[number] =
          {
            questionId: a.questionId,
            formatted:
              formatted.length > 0 ? formatted : [{ text: a.text || "" }],
          };
        if (needsValidation) {
          const v = validations[key];
          if (v === "VALIDATED" || v === "INVALIDATED") {
            item.teacherValidation = v;
          }
        }
        return item;
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
      const updated = await saveActivityFeedback(submissionId, payload);
      hydrate(updated);
      onReviewed();
    } catch (e) {
      const err = e as ApiError;
      if (err.error === "VALIDATION_ERROR") {
        setError(
          needsValidation
            ? t("admin.homeworkReview.validationRequired")
            : t("admin.homeworkReview.validationError"),
        );
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

            {legacy && (
              <p className="text-sm text-muted-foreground">
                {t("admin.homeworkReview.legacyFrozen")}
              </p>
            )}

            {needsValidation && (
              <p className="text-sm text-muted-foreground">
                {isMixed
                  ? t("admin.homeworkReview.mixedHint")
                  : t("admin.homeworkReview.manualValidationHint")}
              </p>
            )}

            {isMixed &&
              submission.result &&
              submission.questions &&
              submission.questions.length > 0 && (
                <div className="space-y-2">
                  <p className="text-sm font-medium">
                    {t("admin.homeworkReview.autoResults")}
                  </p>
                  <ExerciseResult
                    questions={submission.questions.filter(
                      (q) => q.kind !== "FREE_TEXT",
                    )}
                    result={submission.result}
                    showAllAnswers
                  />
                </div>
              )}

            {submission.scorePercent != null && (
              <p className="text-sm font-medium">
                {t("admin.homeworkReview.combinedScore", {
                  percent: submission.scorePercent,
                })}
              </p>
            )}

            <div>
              <p className="mb-1 text-xs font-medium text-muted-foreground">
                {hasMultiAnswers
                  ? t("admin.homeworkReview.studentAnswers")
                  : t("admin.homeworkReview.studentAnswer")}
              </p>
              {hasMultiAnswers ? (
                <ul className="space-y-3">
                  {freeTextAnswers.map((a, i) => {
                    const key = a.questionId ?? `null:${a.promptSnapshot}`;
                    const formatted =
                      answerFormats[key] ?? toFormatted(a.text, a.formatted);
                    const validation = validations[key] ?? null;
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
                        {needsValidation && editable && (
                          <div className="mt-3 flex flex-wrap gap-2">
                            <Button
                              type="button"
                              size="sm"
                              variant={
                                validation === "VALIDATED"
                                  ? "default"
                                  : "outline"
                              }
                              disabled={saving}
                              onClick={() =>
                                setValidations((prev) => ({
                                  ...prev,
                                  [key]: "VALIDATED",
                                }))
                              }
                            >
                              {t("admin.homeworkReview.validate")}
                            </Button>
                            <Button
                              type="button"
                              size="sm"
                              variant={
                                validation === "INVALIDATED"
                                  ? "destructive"
                                  : "outline"
                              }
                              disabled={saving}
                              onClick={() =>
                                setValidations((prev) => ({
                                  ...prev,
                                  [key]: "INVALIDATED",
                                }))
                              }
                            >
                              {t("admin.homeworkReview.invalidate")}
                            </Button>
                          </div>
                        )}
                        {needsValidation && !editable && validation && (
                          <p className="mt-2 text-xs font-medium">
                            {validation === "VALIDATED"
                              ? t("admin.homeworkReview.validatedBadge")
                              : t("admin.homeworkReview.invalidatedBadge")}
                          </p>
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
                  rows={8}
                  disabled={saving}
                />
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
              <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={onClose}>
                  {t("admin.homeworkReview.close")}
                </Button>
                <Button
                  type="button"
                  disabled={saving || (needsValidation && !allValidated)}
                  onClick={handleSave}
                >
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
