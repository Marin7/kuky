import { Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import {
  getUniversityExercise,
  submitUniversityExercise,
} from "@/lib/university";
import type { ExerciseResponse } from "@/lib/learning";

export function UniversityHomeworkExercisePage({ homeworkId }: { homeworkId: string }) {
  const [exercise, setExercise] = useState<ExerciseResponse | null>(null);
  const [answers, setAnswers] = useState<Record<string, string[]>>({});
  const [text, setText] = useState<Record<string, string>>({});
  const [result, setResult] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getUniversityExercise(homeworkId).then(setExercise).catch(() => setError("No se pudo cargar el ejercicio."));
  }, [homeworkId]);

  if (!exercise) return <p className="animate-pulse text-muted-foreground">{error ?? "Cargando ejercicio…"}</p>;

  const submit = async () => {
    try {
      const grade = await submitUniversityExercise(
        homeworkId,
        exercise.questions.map((question) => ({
          questionId: question.id,
          selectedOptionIds: answers[question.id] ?? [],
          answerText: question.kind === "FILL_BLANK" ? text[question.id] ?? "" : null,
        })),
      );
      setResult(grade.scorePercent);
    } catch {
      setError("No se pudo entregar el ejercicio.");
    }
  };

  return (
    <div className="mx-auto max-w-2xl px-6 py-12">
      <Link to="/universidad/aprendizaje" className="text-sm text-muted-foreground hover:text-foreground">← Volver a mi aprendizaje</Link>
      <h1 className="mt-8 font-display text-3xl font-semibold text-primary">{exercise.title}</h1>
      <p className="mt-2 whitespace-pre-wrap text-muted-foreground">{exercise.instructions}</p>
      {result !== null ? <p className="mt-8 text-lg font-medium">Resultado: {result}%</p> : (
        <div className="mt-8 space-y-6">
          {exercise.questions.map((question, index) => (
            <fieldset key={question.id} className="space-y-2">
              <legend className="font-medium">{index + 1}. {question.prompt}</legend>
              {question.kind === "FILL_BLANK" ? (
                <input className="w-full rounded-md border p-2" value={text[question.id] ?? ""} onChange={(e) => setText((all) => ({ ...all, [question.id]: e.target.value }))} />
              ) : question.options.map((option) => {
                const selected = (answers[question.id] ?? []).includes(option.id);
                return <label key={option.id} className="flex items-center gap-2 text-sm">
                  <input
                    type={question.kind === "SINGLE_CHOICE" ? "radio" : "checkbox"}
                    name={question.id}
                    checked={selected}
                    onChange={() => setAnswers((all) => ({
                      ...all,
                      [question.id]: question.kind === "SINGLE_CHOICE"
                        ? [option.id]
                        : selected ? (all[question.id] ?? []).filter((id) => id !== option.id) : [...(all[question.id] ?? []), option.id],
                    }))}
                  />
                  {option.label}
                </label>;
              })}
            </fieldset>
          ))}
          {error && <p className="text-sm text-destructive">{error}</p>}
          <button onClick={submit} className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground">Entregar</button>
        </div>
      )}
    </div>
  );
}
