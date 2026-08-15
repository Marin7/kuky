import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  isHomeworkUpdatedError,
  submitHomeworkAnswers,
  type ApiError,
  type ExerciseResult as ExerciseResultData,
  type HeterogeneousAnswerPayload,
  type ManualAnswerItem,
  type MatchingAnswer,
  type StudentQuestion,
  type HomeworkStatus,
} from "@/lib/learning";
import { countBlanks } from "@/lib/blankTokens";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { ExerciseResult } from "./ExerciseResult";
import { matchClassicInlineSingleChoice } from "@/lib/inlineChoice";
import { NumberedSingleChoiceQuestion } from "./NumberedSingleChoiceQuestion";
import { InlineSingleChoiceQuestion } from "./InlineSingleChoiceQuestion";
import { MultiBlankQuestion } from "./MultiBlankQuestion";
import { DragDropQuestion } from "./DragDropQuestion";
import { TableFillQuestion } from "./TableFillQuestion";
import { MatchingQuestion } from "./MatchingQuestion";
import { RichTextViewer } from "./richtext/RichTextViewer";

interface AnswerState {
  selectedOptionIds: string[];
  selections: Record<string, string>;
  blanks: string[];
  placements: (string | null)[];
  cells: Record<string, string>;
  pairs: MatchingAnswer["pairs"];
  text: string;
}

export interface MixedAssignmentView {
  id: string;
  status: HomeworkStatus;
  questions: StudentQuestion[];
  result: ExerciseResultData | null;
  answers?: ManualAnswerItem[] | null;
  scorePercent?: number | null;
  provisionalScorePercent?: number | null;
  feedbackText?: string | null;
  teacherFeedback?: string | null;
  contentRevisedAt?: string | null;
}

interface Props {
  assignment: MixedAssignmentView;
  onSubmitted?: () => void;
  /** Reload live homework after Paula updates it (homework takes only). */
  onHomeworkUpdated?: () => void;
  /** Override default homework submit (e.g. presentation activities). */
  submitAnswers?: (
    id: string,
    answers: HeterogeneousAnswerPayload[],
  ) => Promise<unknown>;
}

function hidesPromptLabel(
  kind: StudentQuestion["kind"],
  inlineChoice: ReturnType<typeof matchClassicInlineSingleChoice>,
): boolean {
  return kind === "MULTI_BLANK" || kind === "DRAG_DROP" || inlineChoice != null;
}

function numberedItems(q: StudentQuestion) {
  return q.kind === "SINGLE_CHOICE" ? (q.structure?.items ?? []) : [];
}

function initialAnswerState(q: StudentQuestion): AnswerState {
  return {
    selectedOptionIds: [],
    selections: {},
    blanks:
      q.kind === "MULTI_BLANK" ? Array(countBlanks(q.prompt)).fill("") : [],
    placements:
      q.kind === "DRAG_DROP"
        ? Array(
            Math.max(countBlanks(q.prompt), q.structure?.bank?.length ?? 0),
          ).fill(null)
        : [],
    cells: {},
    pairs: [],
    text: "",
  };
}

function isFreeText(kind: StudentQuestion["kind"]): boolean {
  return kind === "FREE_TEXT";
}

/**
 * Single form for MIXED composition: structured controls + FREE_TEXT fields,
 * submitted together via /answers. Post-submit shows auto results + awaiting /
 * finalized validation marks.
 */
export function MixedHomeworkForm({
  assignment,
  onSubmitted,
  onHomeworkUpdated,
  submitAnswers,
}: Props) {
  const { t } = useTranslation();
  const [answers, setAnswers] = useState<Record<string, AnswerState>>(() =>
    Object.fromEntries(
      assignment.questions.map((q) => [q.id, initialAnswerState(q)]),
    ),
  );
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const prevToken = useRef(assignment.contentRevisedAt);

  useEffect(() => {
    const next = assignment.contentRevisedAt;
    if (prevToken.current && next && prevToken.current !== next) {
      setAnswers(
        Object.fromEntries(
          assignment.questions.map((q) => [q.id, initialAnswerState(q)]),
        ),
      );
      setError(t("learning.homeworkUpdated"));
    }
    prevToken.current = next ?? null;
  }, [assignment.contentRevisedAt, assignment.questions, t]);

  const submitted = assignment.status !== "PENDING";
  const finalized =
    assignment.status === "GRADED" || assignment.status === "REVIEWED";
  const freeTextQuestions = assignment.questions.filter((q) =>
    isFreeText(q.kind),
  );
  const autoQuestions = assignment.questions.filter((q) => !isFreeText(q.kind));

  const setSingle = (qId: string, optionId: string | null) =>
    setAnswers((prev) => ({
      ...prev,
      [qId]: {
        ...prev[qId],
        selectedOptionIds: optionId ? [optionId] : [],
      },
    }));

  const setItemSelection = (qId: string, number: number, optionId: string) =>
    setAnswers((prev) => ({
      ...prev,
      [qId]: {
        ...prev[qId],
        selections: { ...prev[qId].selections, [String(number)]: optionId },
      },
    }));

  const toggleMulti = (qId: string, optionId: string, checked: boolean) =>
    setAnswers((prev) => {
      const current = prev[qId].selectedOptionIds;
      const next = checked
        ? [...current, optionId]
        : current.filter((id) => id !== optionId);
      return { ...prev, [qId]: { ...prev[qId], selectedOptionIds: next } };
    });

  const setBlanks = (qId: string, blanks: string[]) =>
    setAnswers((prev) => ({ ...prev, [qId]: { ...prev[qId], blanks } }));

  const setPlacements = (qId: string, placements: (string | null)[]) =>
    setAnswers((prev) => ({ ...prev, [qId]: { ...prev[qId], placements } }));

  const setCells = (qId: string, cells: Record<string, string>) =>
    setAnswers((prev) => ({ ...prev, [qId]: { ...prev[qId], cells } }));

  const setPairs = (qId: string, pairs: MatchingAnswer["pairs"]) =>
    setAnswers((prev) => ({ ...prev, [qId]: { ...prev[qId], pairs } }));

  const setText = (qId: string, text: string) =>
    setAnswers((prev) => ({ ...prev, [qId]: { ...prev[qId], text } }));

  const submit = async () => {
    for (const q of freeTextQuestions) {
      if (!(answers[q.id]?.text ?? "").trim()) {
        setError(t("learning.manualMulti.allRequired"));
        return;
      }
    }
    for (const q of autoQuestions) {
      const items = numberedItems(q);
      if (items.length === 0) continue;
      const selections = answers[q.id]?.selections ?? {};
      if (items.some((item) => !selections[String(item.number)])) {
        setError(t("learning.numberedSingleChoice.allRequired"));
        return;
      }
    }

    setSubmitting(true);
    setError(null);
    try {
      const payload: HeterogeneousAnswerPayload[] = assignment.questions.map(
        (q) => {
          const a = answers[q.id];
          if (isFreeText(q.kind)) {
            return { questionId: q.id, text: (a?.text ?? "").trim() };
          }
          let answerJson: unknown = null;
          const items = numberedItems(q);
          if (items.length > 0)
            answerJson = { selections: a?.selections ?? {} };
          else if (q.kind === "MULTI_BLANK")
            answerJson = { blanks: a?.blanks ?? [] };
          else if (q.kind === "DRAG_DROP")
            answerJson = { placements: a?.placements ?? [] };
          else if (q.kind === "TABLE_FILL")
            answerJson = { cells: a?.cells ?? {} };
          else if (q.kind === "MATCHING")
            answerJson = { pairs: a?.pairs ?? [] };
          return {
            questionId: q.id,
            selectedOptionIds: a?.selectedOptionIds ?? [],
            answerJson,
          };
        },
      );
      if (submitAnswers) {
        await submitAnswers(assignment.id, payload);
      } else {
        await submitHomeworkAnswers(
          assignment.id,
          payload,
          assignment.contentRevisedAt,
        );
      }
      onSubmitted?.();
    } catch (e) {
      const err = e as ApiError;
      if (!submitAnswers && isHomeworkUpdatedError(e)) {
        setAnswers(
          Object.fromEntries(
            assignment.questions.map((q) => [q.id, initialAnswerState(q)]),
          ),
        );
        setError(t("learning.homeworkUpdated"));
        onHomeworkUpdated?.();
      } else if (err.error === "VALIDATION_ERROR") {
        setError(t("learning.manualMulti.allRequired"));
      } else if (err.error === "SUBMISSION_NOT_ALLOWED") {
        setError(t("learning.submitDialog.submissionNotAllowedError"));
      } else {
        setError(err.message ?? t("learning.exercisePage.submitError"));
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (submitted) {
    const autoResult =
      assignment.result ??
      (finalized && assignment.scorePercent != null
        ? {
            scorePercent: assignment.scorePercent,
            fullyCorrectCount: 0,
            totalQuestions: assignment.questions.length,
            questions: [],
          }
        : null);

    return (
      <div className="mt-6 space-y-5">
        {finalized && assignment.scorePercent != null ? (
          <div className="rounded-lg border bg-card p-4">
            <p className="text-2xl font-semibold text-primary">
              {assignment.scorePercent}%
            </p>
            <p className="text-sm text-muted-foreground">
              {t("learning.mixed.combinedScore")}
            </p>
          </div>
        ) : (
          <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 dark:border-amber-900 dark:bg-amber-950/40">
            <p className="text-sm font-medium text-amber-900 dark:text-amber-100">
              {t("learning.mixed.awaitingTeacher")}
            </p>
            {assignment.provisionalScorePercent != null && (
              <p className="mt-1 text-sm text-amber-800 dark:text-amber-200">
                {t("learning.mixed.provisionalScore", {
                  percent: assignment.provisionalScorePercent,
                })}
              </p>
            )}
          </div>
        )}

        {autoResult && autoQuestions.length > 0 && (
          <div className="space-y-2">
            <p className="text-base font-medium text-foreground">
              {t("learning.mixed.autoResults")}
            </p>
            <ExerciseResult
              questions={autoQuestions}
              result={
                autoResult.questions.length > 0
                  ? autoResult
                  : {
                      ...autoResult,
                      questions: [],
                      totalQuestions: autoQuestions.length,
                    }
              }
              teacherFeedback={finalized ? null : assignment.teacherFeedback}
            />
          </div>
        )}

        {freeTextQuestions.length > 0 && (
          <div className="space-y-3">
            <p className="text-base font-medium text-foreground">
              {t("learning.mixed.manualAnswers")}
            </p>
            <ul className="space-y-3">
              {freeTextQuestions.map((q, i) => {
                const ans = assignment.answers?.find(
                  (a) => a.questionId === q.id,
                );
                const percent = ans?.teacherScorePercent;
                return (
                  <li
                    key={q.id}
                    className="space-y-2 rounded-md border bg-muted/20 p-3"
                  >
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-sm font-medium">
                        {t("learning.manualMulti.questionLabel", {
                          index: i + 1,
                        })}
                        {q.prompt ? ` — ${q.prompt}` : ""}
                      </p>
                      {finalized && percent != null && (
                        <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-foreground">
                          {t("learning.mixed.scorePercent", { percent })}
                        </span>
                      )}
                      {!finalized && (
                        <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
                          {t("learning.mixed.pendingValidation")}
                        </span>
                      )}
                    </div>
                    <div className="text-sm break-all [overflow-wrap:anywhere]">
                      {ans?.formatted && ans.formatted.length > 0 ? (
                        <RichTextViewer segments={ans.formatted} />
                      ) : (
                        <p className="whitespace-pre-wrap">
                          {ans?.text || t("learning.manualMulti.emptyAnswer")}
                        </p>
                      )}
                    </div>
                  </li>
                );
              })}
            </ul>
          </div>
        )}

        {assignment.feedbackText ? (
          <div className="space-y-1">
            <p className="text-sm font-medium">
              {t("learning.writePage.teacherFeedback")}
            </p>
            <div className="rounded-md border bg-muted/20 p-3 text-sm whitespace-pre-wrap break-all [overflow-wrap:anywhere]">
              {assignment.feedbackText}
            </div>
          </div>
        ) : assignment.teacherFeedback?.trim() ? (
          <div className="space-y-1">
            <p className="text-sm font-medium">
              {t("learning.writePage.teacherFeedback")}
            </p>
            <div className="rounded-md border bg-muted/20 p-3 text-sm whitespace-pre-wrap">
              {assignment.teacherFeedback}
            </div>
          </div>
        ) : null}
      </div>
    );
  }

  return (
    <div className="mt-6 space-y-5">
      <div
        className={assignment.questions.length > 1 ? "space-y-8" : "space-y-5"}
      >
        {assignment.questions.map((q, i) => {
          const inlineChoice = matchClassicInlineSingleChoice(q);
          return (
            <div key={q.id} className="space-y-2.5">
              {isFreeText(q.kind) ? (
                <>
                  <Label
                    htmlFor={`mixed-ft-${q.id}`}
                    className="block whitespace-pre-wrap text-base font-medium leading-relaxed"
                  >
                    {`${i + 1}. ${q.prompt}`}
                  </Label>
                  <Textarea
                    id={`mixed-ft-${q.id}`}
                    value={answers[q.id]?.text ?? ""}
                    onChange={(e) => setText(q.id, e.target.value)}
                    rows={3}
                    disabled={submitting}
                    placeholder={t("learning.manualMulti.placeholder")}
                    maxLength={2000}
                  />
                </>
              ) : (
                <>
                  {!hidesPromptLabel(q.kind, inlineChoice) && (
                    <Label className="block whitespace-pre-wrap text-base font-medium leading-relaxed">
                      {`${i + 1}. ${q.prompt}`}
                    </Label>
                  )}

                  {q.kind === "SINGLE_CHOICE" &&
                    numberedItems(q).length > 0 && (
                      <NumberedSingleChoiceQuestion
                        questionId={q.id}
                        items={numberedItems(q)}
                        selections={answers[q.id]?.selections ?? {}}
                        onChange={(number, optionId) =>
                          setItemSelection(q.id, number, optionId)
                        }
                      />
                    )}

                  {inlineChoice && (
                    <InlineSingleChoiceQuestion
                      number={i + 1}
                      prompt={q.prompt}
                      match={inlineChoice}
                      selectedOptionId={
                        answers[q.id]?.selectedOptionIds[0] ?? null
                      }
                      onChange={(optionId) => setSingle(q.id, optionId)}
                    />
                  )}

                  {((q.kind === "SINGLE_CHOICE" &&
                    numberedItems(q).length === 0 &&
                    !inlineChoice) ||
                    q.kind === "TRUE_FALSE") && (
                    <RadioGroup
                      value={answers[q.id]?.selectedOptionIds[0] ?? ""}
                      onValueChange={(v) => setSingle(q.id, v)}
                    >
                      {q.options.map((o) => (
                        <label
                          key={o.id}
                          className="flex items-center gap-2.5 text-base leading-snug"
                        >
                          <RadioGroupItem value={o.id} id={`${q.id}-${o.id}`} />
                          {q.kind === "TRUE_FALSE"
                            ? t(
                                o.label === "false"
                                  ? "learning.trueFalse.false"
                                  : "learning.trueFalse.true",
                              )
                            : o.label}
                        </label>
                      ))}
                    </RadioGroup>
                  )}

                  {q.kind === "MULTI_CHOICE" && (
                    <div className="space-y-2.5">
                      {q.options.map((o) => (
                        <label
                          key={o.id}
                          className="flex items-center gap-2.5 text-base leading-snug"
                        >
                          <Checkbox
                            checked={answers[q.id]?.selectedOptionIds.includes(
                              o.id,
                            )}
                            onCheckedChange={(c) =>
                              toggleMulti(q.id, o.id, c === true)
                            }
                          />
                          {o.label}
                        </label>
                      ))}
                    </div>
                  )}

                  {q.kind === "MULTI_BLANK" && (
                    <MultiBlankQuestion
                      number={i + 1}
                      prompt={q.prompt}
                      value={answers[q.id]?.blanks ?? []}
                      onChange={(blanks) => setBlanks(q.id, blanks)}
                    />
                  )}

                  {q.kind === "DRAG_DROP" && (
                    <DragDropQuestion
                      number={i + 1}
                      prompt={q.prompt}
                      bank={q.structure?.bank ?? []}
                      value={answers[q.id]?.placements ?? []}
                      onChange={(placements) => setPlacements(q.id, placements)}
                    />
                  )}

                  {q.kind === "TABLE_FILL" && (
                    <TableFillQuestion
                      structure={q.structure ?? {}}
                      value={answers[q.id]?.cells ?? {}}
                      onChange={(cells) => setCells(q.id, cells)}
                    />
                  )}

                  {q.kind === "MATCHING" && (
                    <MatchingQuestion
                      left={q.structure?.left ?? []}
                      right={q.structure?.right ?? []}
                      pairs={answers[q.id]?.pairs ?? []}
                      onChange={(pairs) => setPairs(q.id, pairs)}
                    />
                  )}
                </>
              )}
            </div>
          );
        })}
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <Button onClick={submit} disabled={submitting}>
        {submitting
          ? t("learning.exercisePage.submitting")
          : t("learning.exercisePage.submit")}
      </Button>
    </div>
  );
}
