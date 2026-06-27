import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  submitExercise,
  type ExerciseResponse,
  type ExerciseResult as ExerciseResultData,
  type AnswerPayload,
  type ApiError,
} from "@/lib/learning";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { ExerciseResult } from "./ExerciseResult";

interface AnswerState {
  selectedOptionIds: string[];
  answerText: string;
}

interface Props {
  exercise: ExerciseResponse;
}

/**
 * Renders the answerable questions of an auto-graded exercise (or its result
 * once graded). Shared by the grammar exercise page and the reading page.
 */
export function ExerciseForm({ exercise }: Props) {
  const { t } = useTranslation();
  const [answers, setAnswers] = useState<Record<string, AnswerState>>(() =>
    Object.fromEntries(
      exercise.questions.map((q) => [
        q.id,
        { selectedOptionIds: [], answerText: "" },
      ]),
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

  const setText = (qId: string, text: string) =>
    setAnswers((prev) => ({
      ...prev,
      [qId]: { ...prev[qId], answerText: text },
    }));

  const submit = async () => {
    setSubmitting(true);
    setError(null);
    try {
      const payload: AnswerPayload[] = exercise.questions.map((q) => ({
        questionId: q.id,
        selectedOptionIds: answers[q.id]?.selectedOptionIds ?? [],
        answerText:
          q.kind === "FILL_BLANK" ? (answers[q.id]?.answerText ?? "") : null,
      }));
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
      <div className="mt-8 space-y-4">
        <p className="text-sm font-medium text-foreground">
          {t("learning.exercisePage.result")}
        </p>
        <ExerciseResult questions={exercise.questions} result={result} />
      </div>
    );
  }

  return (
    <div className="mt-8 space-y-6">
      {exercise.questions.map((q, i) => (
        <div key={q.id} className="space-y-2">
          <Label className="text-sm font-medium">
            {i + 1}. {q.prompt}
          </Label>

          {q.kind === "SINGLE_CHOICE" && (
            <RadioGroup
              value={answers[q.id]?.selectedOptionIds[0] ?? ""}
              onValueChange={(v) => setSingle(q.id, v)}
            >
              {q.options.map((o) => (
                <label key={o.id} className="flex items-center gap-2 text-sm">
                  <RadioGroupItem value={o.id} id={`${q.id}-${o.id}`} />
                  {o.label}
                </label>
              ))}
            </RadioGroup>
          )}

          {q.kind === "MULTI_CHOICE" && (
            <div className="space-y-2">
              {q.options.map((o) => (
                <label key={o.id} className="flex items-center gap-2 text-sm">
                  <Checkbox
                    checked={answers[q.id]?.selectedOptionIds.includes(o.id)}
                    onCheckedChange={(c) => toggleMulti(q.id, o.id, c === true)}
                  />
                  {o.label}
                </label>
              ))}
            </div>
          )}

          {q.kind === "FILL_BLANK" && (
            <Input
              value={answers[q.id]?.answerText ?? ""}
              onChange={(e) => setText(q.id, e.target.value)}
              placeholder={t("learning.exercisePage.yourAnswer")}
              className="max-w-sm"
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
