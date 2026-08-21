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
import { resolveComposition } from "@/lib/learning";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  preventDialogDismissForPopover,
  TextareaWithEmoji,
} from "@/components/learning/richtext/ClassroomEmojiPicker";
import { RichTextEditor } from "@/components/learning/richtext/RichTextEditor";
import { RichTextViewer } from "@/components/learning/richtext/RichTextViewer";
import { TextWithLinks } from "@/components/learning/TextWithLinks";
import { ExerciseResult } from "@/components/learning/ExerciseResult";
import {
  plainText,
  type FormattedText,
} from "@/components/learning/richtext/types";
import { notifyBadgesChanged } from "@/lib/notifications";

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
    composition === "MIXED" ||
    composition === "ALL_MANUAL" ||
    composition === "WRITE";
  return (
    s.status === "SUBMITTED" ||
    (s.status === "REVIEWED" && s.reviewModel === "ANNOTATED") ||
    (s.status === "GRADED" && scoredManual && s.reviewModel === "ANNOTATED")
  );
}

function isFreeTextAnswer(a: { kind?: string | null; text?: string }): boolean {
  return !a.kind || a.kind === "FREE_TEXT";
}

function parsePercentInput(raw: string): number | null {
  const trimmed = raw.trim();
  if (trimmed === "") return null;
  if (!/^\d{1,3}$/.test(trimmed)) return NaN;
  const n = Number(trimmed);
  if (!Number.isInteger(n) || n < 0 || n > 100) return NaN;
  return n;
}

function percentToInput(value: number | null | undefined): string {
  return value == null ? "" : String(value);
}

/**
 * MANUAL / MIXED / WRITE homework review: annotate + optional plain ≤500 note.
 * FREE_TEXT (and WRITE) answers use 0–100% teacher scores (partial save or finalize).
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
  const [percents, setPercents] = useState<Record<string, string>>({});
  const [writePercent, setWritePercent] = useState("");
  const [feedbackText, setFeedbackText] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const hydrate = (data: HomeworkSubmissionAdmin) => {
    setSubmission(data);
    setResponse(data.response ?? []);
    const map: Record<string, FormattedText> = {};
    const pcts: Record<string, string> = {};
    for (const a of data.answers ?? []) {
      const key = a.questionId ?? `null:${a.promptSnapshot}`;
      map[key] = toFormatted(a.text, a.formatted);
      pcts[key] = percentToInput(a.teacherScorePercent);
    }
    setAnswerFormats(map);
    setPercents(pcts);
    const composition = resolveComposition(data);
    if (composition === "WRITE") {
      setWritePercent(
        percentToInput(
          data.teacherScorePercent ??
            (data.status === "GRADED" ? data.scorePercent : null),
        ),
      );
    } else {
      setWritePercent("");
    }
    setFeedbackText(data.feedbackText ?? "");
  };

  useEffect(() => {
    getHomeworkSubmission(submissionId)
      .then((data) => {
        hydrate(data);
        notifyBadgesChanged();
      })
      .catch(() => setLoadError(t("admin.homeworkReview.loadError")))
      .finally(() => setLoading(false));
  }, [submissionId, t]);

  const multiAnswers = submission?.answers?.filter(Boolean) ?? [];
  const freeTextAnswers = multiAnswers.filter(isFreeTextAnswer);
  const hasMultiAnswers = freeTextAnswers.length > 0;
  const composition = submission ? resolveComposition(submission) : null;
  const isMixed = composition === "MIXED";
  const needsPercent =
    composition === "MIXED" ||
    composition === "ALL_MANUAL" ||
    composition === "WRITE";
  const legacy = submission?.reviewModel === "LEGACY_RICH";
  const editable = submission ? isEditableReview(submission) : false;
  const isGraded = submission?.status === "GRADED";

  const allPercentsSet = !needsPercent
    ? true
    : hasMultiAnswers
      ? freeTextAnswers.every((a) => {
          const key = a.questionId ?? `null:${a.promptSnapshot}`;
          const n = parsePercentInput(percents[key] ?? "");
          return n !== null && !Number.isNaN(n);
        })
      : (() => {
          const n = parsePercentInput(writePercent);
          return n !== null && !Number.isNaN(n);
        })();

  const save = async (finalize: boolean) => {
    if (!submission || !editable) return;
    const trimmed = feedbackText.trim();
    if (trimmed.length > MAX_FEEDBACK) {
      setError(t("admin.homeworkReview.feedbackTooLong"));
      return;
    }
    if (finalize && needsPercent && !allPercentsSet) {
      setError(t("admin.homeworkReview.percentRequired"));
      return;
    }
    if (isGraded && !finalize) {
      setError(t("admin.homeworkReview.percentRequired"));
      return;
    }

    const payload: SaveHomeworkReviewPayload = {
      feedbackText: trimmed,
      finalize: isGraded ? true : finalize,
    };
    if (hasMultiAnswers) {
      payload.answers = freeTextAnswers.map((a) => {
        const key = a.questionId ?? `null:${a.promptSnapshot}`;
        const formatted =
          answerFormats[key] ?? toFormatted(a.text, a.formatted);
        const item: NonNullable<SaveHomeworkReviewPayload["answers"]>[number] =
          {
            questionId: a.questionId,
            formatted:
              formatted.length > 0 ? formatted : [{ text: a.text || "" }],
          };
        const n = parsePercentInput(percents[key] ?? "");
        if (Number.isNaN(n)) {
          throw new Error("INVALID_PERCENT");
        }
        if (n !== null) item.teacherScorePercent = n;
        return item;
      });
    } else {
      payload.response =
        response.length > 0
          ? response
          : [{ text: plainText(submission.response ?? []) || " " }];
      const n = parsePercentInput(writePercent);
      if (Number.isNaN(n)) {
        setError(t("admin.homeworkReview.percentInvalid"));
        return;
      }
      if (n !== null) payload.teacherScorePercent = n;
    }

    setSaving(true);
    setError(null);
    try {
      const updated = await saveHomeworkFeedback(submissionId, payload);
      hydrate(updated);
      onReviewed();
    } catch (e) {
      if (e instanceof Error && e.message === "INVALID_PERCENT") {
        setError(t("admin.homeworkReview.percentInvalid"));
        return;
      }
      const err = e as ApiError;
      if (err.error === "VALIDATION_ERROR") {
        setError(
          needsPercent
            ? t("admin.homeworkReview.percentRequired")
            : t("admin.homeworkReview.validationError"),
        );
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

  const percentField = (
    value: string,
    onChange: (next: string) => void,
    readOnlyDisplay?: number | null,
  ) => {
    if (!editable) {
      if (readOnlyDisplay == null) return null;
      return (
        <p className="mt-2 text-xs font-medium">
          {t("admin.homeworkReview.percentBadge", { percent: readOnlyDisplay })}
        </p>
      );
    }
    return (
      <div className="mt-3 flex items-center gap-2">
        <label className="text-xs font-medium text-muted-foreground">
          {t("admin.homeworkReview.percentLabel")}
        </label>
        <Input
          type="text"
          inputMode="numeric"
          className="h-8 w-20"
          value={value}
          disabled={saving}
          onChange={(e) => onChange(e.target.value)}
          placeholder="0–100"
          aria-label={t("admin.homeworkReview.percentLabel")}
        />
        <span className="text-xs text-muted-foreground">%</span>
      </div>
    );
  };

  return (
    <Dialog open onOpenChange={(next) => !next && onClose()}>
      <DialogContent
        className="max-h-[85vh] max-w-2xl overflow-y-auto"
        onPointerDownOutside={preventDialogDismissForPopover}
        onInteractOutside={preventDialogDismissForPopover}
      >
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

            {needsPercent && (
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
              <p className="mb-2 text-sm font-medium">
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
                        {needsPercent &&
                          percentField(
                            percents[key] ?? "",
                            (next) =>
                              setPercents((prev) => ({
                                ...prev,
                                [key]: next,
                              })),
                            a.teacherScorePercent,
                          )}
                      </li>
                    );
                  })}
                </ul>
              ) : (
                <div className="space-y-3">
                  {editable ? (
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
                  {needsPercent &&
                    percentField(
                      writePercent,
                      setWritePercent,
                      submission.teacherScorePercent ?? submission.scorePercent,
                    )}
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
                  <TextareaWithEmoji
                    value={feedbackText}
                    onChange={setFeedbackText}
                    placeholder={t("admin.homeworkReview.feedbackPlaceholder")}
                    rows={3}
                    maxLength={MAX_FEEDBACK}
                    disabled={saving}
                    allowEmojiInsert
                    className="break-all [overflow-wrap:anywhere]"
                  />
                  <p className="text-right text-xs text-muted-foreground tabular-nums">
                    {feedbackText.trim().length} / {MAX_FEEDBACK}
                  </p>
                </div>
              ) : submission.feedbackText ? (
                <div className="rounded-md border bg-muted/20 p-3 text-sm whitespace-pre-wrap break-all [overflow-wrap:anywhere]">
                  <TextWithLinks text={submission.feedbackText} />
                </div>
              ) : null}
            </div>

            {error && <p className="text-sm text-destructive">{error}</p>}

            {editable && (
              <div className="flex flex-wrap justify-end gap-3">
                <Button variant="outline" onClick={onClose} disabled={saving}>
                  {t("admin.homeworkReview.close")}
                </Button>
                {!isGraded && (
                  <Button
                    variant="secondary"
                    onClick={() => void save(false)}
                    disabled={saving}
                  >
                    {saving
                      ? t("admin.homeworkReview.saving")
                      : t("admin.homeworkReview.saveProgress")}
                  </Button>
                )}
                <Button
                  onClick={() => void save(true)}
                  disabled={saving || (needsPercent && !allPercentsSet)}
                >
                  {saving
                    ? t("admin.homeworkReview.saving")
                    : isGraded
                      ? t("admin.homeworkReview.save")
                      : t("admin.homeworkReview.finalize")}
                </Button>
              </div>
            )}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
