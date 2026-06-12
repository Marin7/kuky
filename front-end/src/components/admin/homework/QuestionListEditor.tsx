import type { AdminQuestion } from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { QuestionEditorCard } from "./QuestionEditorCard";

interface Props {
  questions: AdminQuestion[];
  onChange: (questions: AdminQuestion[]) => void;
}

function newQuestion(): AdminQuestion {
  return {
    kind: "SINGLE_CHOICE",
    prompt: "",
    options: [
      { label: "", correct: false },
      { label: "", correct: false },
    ],
  };
}

export function QuestionListEditor({ questions, onChange }: Props) {
  const update = (i: number, q: AdminQuestion) =>
    onChange(questions.map((existing, idx) => (idx === i ? q : existing)));

  const remove = (i: number) =>
    onChange(questions.filter((_, idx) => idx !== i));

  const move = (i: number, delta: number) => {
    const j = i + delta;
    if (j < 0 || j >= questions.length) return;
    const next = [...questions];
    [next[i], next[j]] = [next[j], next[i]];
    onChange(next);
  };

  const add = () => onChange([...questions, newQuestion()]);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold">Preguntas</h3>
        <span className="text-xs text-muted-foreground">
          {questions.length} {questions.length === 1 ? "pregunta" : "preguntas"}
        </span>
      </div>

      {questions.length === 0 ? (
        <p className="rounded-md border border-dashed p-4 text-sm text-muted-foreground">
          Aún no has añadido preguntas. Un ejercicio autocorregible necesita al
          menos una.
        </p>
      ) : (
        questions.map((q, i) => (
          <QuestionEditorCard
            key={i}
            index={i}
            count={questions.length}
            question={q}
            onChange={(updated) => update(i, updated)}
            onRemove={() => remove(i)}
            onMoveUp={() => move(i, -1)}
            onMoveDown={() => move(i, 1)}
          />
        ))
      )}

      <Button type="button" variant="outline" size="sm" onClick={add}>
        Añadir pregunta
      </Button>
    </div>
  );
}
