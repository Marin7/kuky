import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  submitExercise,
  type ExerciseResponse,
  type ExerciseResult as ExerciseResultData,
  type AnswerPayload,
  type ApiError,
  type MatchingAnswer,
} from "@/lib/learning";
import { countBlanks } from "@/lib/blankTokens";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { ExerciseResult } from "./ExerciseResult";
import { MultiBlankQuestion } from "./MultiBlankQuestion";
import { DragDropQuestion } from "./DragDropQuestion";
import { TableFillQuestion } from "./TableFillQuestion";
import { MatchingQuestion } from "./MatchingQuestion";

interface AnswerState {
  selectedOptionIds: string[];
  blanks: string[]; // MULTI_BLANK
  placements: (string | null)[]; // DRAG_DROP
  cells: Record<string, string>; // TABLE_FILL
  pairs: MatchingAnswer["pairs"]; // MATCHING
}

interface Props {
  exercise: ExerciseResponse;
}

// Passage kinds render the number inline with the prompt; skip the separate
// numbered label above the question.
const RENDERS_OWN_PASSAGE = new Set(["MULTI_BLANK", "DRAG_DROP"]);

function initialAnswerState(
  q: ExerciseResponse["questions"][number],
): AnswerState {
  return {
    selectedOptionIds: [],
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
  };
}

/**
 * Renders the answerable questions of an auto-graded exercise (or its result
 * once graded). Shared by the grammar exercise page and the reading page.
 */
export function ExerciseForm({ exercise }: Props) {
  const { t } = useTranslation();
  const [answers, setAnswers] = useState<Record<string, AnswerState>>(() =>
    Object.fromEntries(
      exercise.questions.map((q) => [q.id, initialAnswerState(q)]),
    ),
  );
  const [result, setResult] = useState<ExerciseResultData | null>(
    exercise.status === "GRADED" ? exercise.result : null,
  );
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const setSingle = (qId: string, optionId: string) =>
    setAnswers((prev) => ({
      ...prev,
      [qId]: { ...prev[qId], selectedOptionIds: [optionId] },
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

  const submit = async () => {
    setSubmitting(true);
    setError(null);
    try {
      const payload: AnswerPayload[] = exercise.questions.map((q) => {
        const a = answers[q.id];
        let answerJson: unknown = null;
        if (q.kind === "MULTI_BLANK") answerJson = { blanks: a?.blanks ?? [] };
        else if (q.kind === "DRAG_DROP")
          answerJson = { placements: a?.placements ?? [] };
        else if (q.kind === "TABLE_FILL")
          answerJson = { cells: a?.cells ?? {} };
        else if (q.kind === "MATCHING") answerJson = { pairs: a?.pairs ?? [] };

        return {
          questionId: q.id,
          selectedOptionIds: a?.selectedOptionIds ?? [],
          answerJson,
        };
      });
      const res = await submitExercise(exercise.id, payload);
      setResult(res);
    } catch (e) {
      setError(
        (e as ApiError).message ?? t("learning.exercisePage.submitError"),
      );
    } finally {
      setSubmitting(false);
    }
  };

  if (result) {
    return (
      <div className="mt-6 space-y-4">
        <p className="text-base font-medium text-foreground">
          {t("learning.exercisePage.result")}
        </p>
        <ExerciseResult
          questions={exercise.questions}
          result={result}
          teacherFeedback={exercise.teacherFeedback}
        />
      </div>
    );
  }

  return (
    <div className="mt-6 space-y-5">
      {exercise.questions.map((q, i) => (
        <div key={q.id} className="space-y-2.5">
          {!RENDERS_OWN_PASSAGE.has(q.kind) && (
            <Label className="block whitespace-pre-wrap text-base font-medium leading-relaxed">
              {`${i + 1}. ${q.prompt}`}
            </Label>
          )}

          {q.kind === "SINGLE_CHOICE" && (
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
                  {o.label}
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
                    checked={answers[q.id]?.selectedOptionIds.includes(o.id)}
                    onCheckedChange={(c) => toggleMulti(q.id, o.id, c === true)}
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
        </div>
      ))}

      {error && <p className="text-sm text-destructive">{error}</p>}

      <Button onClick={submit} disabled={submitting}>
        {submitting
          ? t("learning.exercisePage.submitting")
          : t("learning.exercisePage.submit")}
      </Button>
    </div>
  );
}
