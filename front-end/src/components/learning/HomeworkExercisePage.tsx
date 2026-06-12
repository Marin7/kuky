import { useEffect, useState } from "react";
import { Link } from "@tanstack/react-router";
import {
  getExercise,
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
  homeworkId: string;
}

export function HomeworkExercisePage({ homeworkId }: Props) {
  const [exercise, setExercise] = useState<ExerciseResponse | null>(null);
  const [answers, setAnswers] = useState<Record<string, AnswerState>>({});
  const [result, setResult] = useState<ExerciseResultData | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getExercise(homeworkId)
      .then((ex) => {
        setExercise(ex);
        if (ex.status === "GRADED" && ex.result) setResult(ex.result);
        setAnswers(
          Object.fromEntries(
            ex.questions.map((q) => [
              q.id,
              { selectedOptionIds: [], answerText: "" },
            ]),
          ),
        );
      })
      .catch(() => setError("No se pudo cargar el ejercicio."))
      .finally(() => setLoading(false));
  }, [homeworkId]);

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
    if (!exercise) return;
    setSubmitting(true);
    setError(null);
    try {
      const payload: AnswerPayload[] = exercise.questions.map((q) => ({
        questionId: q.id,
        selectedOptionIds: answers[q.id]?.selectedOptionIds ?? [],
        answerText:
          q.kind === "FILL_BLANK" ? (answers[q.id]?.answerText ?? "") : null,
      }));
      const res = await submitExercise(homeworkId, payload);
      setResult(res);
    } catch (e) {
      setError((e as ApiError).message ?? "No se pudo entregar el ejercicio.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto max-w-2xl px-6 py-12">
      <Link
        to="/aprendizaje"
        className="mb-8 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        ← Volver a mi aprendizaje
      </Link>

      {loading && (
        <p className="mt-6 animate-pulse text-sm text-muted-foreground">
          Cargando ejercicio…
        </p>
      )}

      {error && !exercise && (
        <p className="mt-6 text-sm text-destructive">{error}</p>
      )}

      {exercise && (
        <>
          <h1 className="font-display text-3xl font-semibold text-primary">
            {exercise.title}
          </h1>
          <p className="mt-2 whitespace-pre-wrap text-muted-foreground">
            {exercise.instructions}
          </p>

          {result ? (
            <div className="mt-8 space-y-4">
              <p className="text-sm font-medium text-foreground">Resultado</p>
              <ExerciseResult questions={exercise.questions} result={result} />
            </div>
          ) : (
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
                        <label
                          key={o.id}
                          className="flex items-center gap-2 text-sm"
                        >
                          <RadioGroupItem value={o.id} id={`${q.id}-${o.id}`} />
                          {o.label}
                        </label>
                      ))}
                    </RadioGroup>
                  )}

                  {q.kind === "MULTI_CHOICE" && (
                    <div className="space-y-2">
                      {q.options.map((o) => (
                        <label
                          key={o.id}
                          className="flex items-center gap-2 text-sm"
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

                  {q.kind === "FILL_BLANK" && (
                    <Input
                      value={answers[q.id]?.answerText ?? ""}
                      onChange={(e) => setText(q.id, e.target.value)}
                      placeholder="Tu respuesta"
                      className="max-w-sm"
                    />
                  )}
                </div>
              ))}

              {error && <p className="text-sm text-destructive">{error}</p>}

              <Button onClick={submit} disabled={submitting}>
                {submitting ? "Entregando…" : "Entregar"}
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
