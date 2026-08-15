import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "@tanstack/react-router";
import {
  isHomeworkUpdatedError,
  submitHomework,
  type ApiError,
  type ManualAnswerItem,
  type ManualAnswerPayload,
} from "@/lib/learning";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { RichTextViewer } from "@/components/learning/richtext/RichTextViewer";

interface QuestionPrompt {
  id: string;
  prompt: string;
}

interface Props {
  homeworkId: string;
  questions: QuestionPrompt[];
  initialAnswers?: ManualAnswerItem[] | null;
  readOnly: boolean;
  onSubmitted?: () => void;
  /** Where to go after a successful submit. Default: stay on the page. */
  redirectTo?: "/aprendizaje" | null;
  contentRevisedAt?: string | null;
  onHomeworkUpdated?: () => void;
  /** Override default homework submit (e.g. presentation activities). */
  submitAnswer?: (
    id: string,
    response?: unknown,
    answers?: ManualAnswerPayload[],
  ) => Promise<unknown>;
}

function initialTextMap(
  questions: QuestionPrompt[],
  initialAnswers?: ManualAnswerItem[] | null,
): Record<string, string> {
  const map: Record<string, string> = {};
  for (const q of questions) {
    map[q.id] = "";
  }
  if (initialAnswers) {
    for (const a of initialAnswers) {
      if (a.questionId in map) {
        map[a.questionId] = a.text;
      }
    }
  }
  return map;
}

/**
 * Compact plain-text answers for MANUAL non-WRITE homework/activities.
 * Every question must be non-empty before submit.
 */
export function ManualMultiAnswerForm({
  homeworkId,
  questions,
  initialAnswers,
  readOnly,
  onSubmitted,
  redirectTo = null,
  contentRevisedAt,
  onHomeworkUpdated,
  submitAnswer,
}: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [texts, setTexts] = useState<Record<string, string>>(() =>
    initialTextMap(questions, initialAnswers),
  );
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const prevToken = useRef(contentRevisedAt);

  useEffect(() => {
    const next = contentRevisedAt;
    if (prevToken.current && next && prevToken.current !== next) {
      setTexts(initialTextMap(questions, null));
      setError(t("learning.homeworkUpdated"));
    }
    prevToken.current = next ?? null;
  }, [contentRevisedAt, questions, t]);

  const setText = (questionId: string, value: string) => {
    setTexts((prev) => ({ ...prev, [questionId]: value }));
  };

  const handleSubmit = async () => {
    const answers: ManualAnswerPayload[] = questions.map((q) => ({
      questionId: q.id,
      text: (texts[q.id] ?? "").trim(),
    }));
    if (answers.some((a) => a.text.length === 0)) {
      setError(t("learning.manualMulti.allRequired"));
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      if (submitAnswer) {
        await submitAnswer(homeworkId, undefined, answers);
      } else {
        await submitHomework(homeworkId, undefined, answers, contentRevisedAt);
      }
      onSubmitted?.();
      if (redirectTo != null) {
        await navigate({ to: redirectTo });
      }
    } catch (e) {
      const err = e as ApiError;
      if (!submitAnswer && isHomeworkUpdatedError(e)) {
        setTexts(initialTextMap(questions, null));
        setError(t("learning.homeworkUpdated"));
        onHomeworkUpdated?.();
      } else if (err.error === "VALIDATION_ERROR") {
        setError(t("learning.manualMulti.allRequired"));
      } else if (err.error === "SUBMISSION_NOT_ALLOWED") {
        setError(t("learning.submitDialog.submissionNotAllowedError"));
      } else {
        setError(t("learning.submitDialog.genericError"));
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (questions.length === 0) {
    return (
      <p className="mt-6 text-sm text-muted-foreground">
        {t("learning.manualMulti.noQuestions")}
      </p>
    );
  }

  return (
    <div className="mt-6 space-y-4">
      <p className="text-base font-medium text-foreground">
        {t("learning.manualMulti.title")}
      </p>

      <div className={questions.length > 1 ? "space-y-8" : "space-y-4"}>
        {questions.map((q, i) => {
          const answerText = texts[q.id] ?? "";
          const readOnlyAnswer =
            initialAnswers?.find((a) => a.questionId === q.id)?.text ??
            answerText;

          return (
            <div key={q.id} className="space-y-1.5">
              <Label
                htmlFor={`manual-ans-${q.id}`}
                className="text-sm font-medium"
              >
                {t("learning.manualMulti.questionLabel", { index: i + 1 })}
                {q.prompt ? ` — ${q.prompt}` : ""}
              </Label>
              {readOnly ? (
                <div className="space-y-1.5">
                  {(() => {
                    const ans = initialAnswers?.find(
                      (a) => a.questionId === q.id,
                    );
                    const percent = ans?.teacherScorePercent;
                    return (
                      <>
                        {percent != null && (
                          <span className="inline-block rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-foreground">
                            {t("learning.mixed.scorePercent", { percent })}
                          </span>
                        )}
                        <div className="rounded-md border bg-muted/20 px-3 py-2 text-sm break-all [overflow-wrap:anywhere]">
                          {ans?.formatted && ans.formatted.length > 0 ? (
                            <RichTextViewer segments={ans.formatted} />
                          ) : (
                            <p className="whitespace-pre-wrap">
                              {readOnlyAnswer ||
                                t("learning.manualMulti.emptyAnswer")}
                            </p>
                          )}
                        </div>
                      </>
                    );
                  })()}
                </div>
              ) : (
                <Textarea
                  id={`manual-ans-${q.id}`}
                  value={answerText}
                  onChange={(e) => setText(q.id, e.target.value)}
                  rows={2}
                  disabled={submitting}
                  placeholder={t("learning.manualMulti.placeholder")}
                  maxLength={2000}
                />
              )}
            </div>
          );
        })}
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      {!readOnly && (
        <div className="flex items-center gap-3 pt-1">
          <Button onClick={handleSubmit} disabled={submitting}>
            {submitting
              ? t("learning.manualMulti.submitting")
              : t("learning.manualMulti.submit")}
          </Button>
        </div>
      )}
    </div>
  );
}
