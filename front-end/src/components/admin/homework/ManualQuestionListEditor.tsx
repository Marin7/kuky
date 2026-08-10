import { useTranslation } from "react-i18next";
import type { AdminQuestion } from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface Props {
  questions: AdminQuestion[];
  onChange: (questions: AdminQuestion[]) => void;
}

function emptyFreeTextQuestion(): AdminQuestion {
  return {
    kind: "FREE_TEXT",
    prompt: "",
    options: [],
    structure: {},
  };
}

/**
 * Ordered prompt-only editor for MANUAL (non-WRITE) free-text questions.
 * No kind picker or options — every item is FREE_TEXT.
 */
export function ManualQuestionListEditor({ questions, onChange }: Props) {
  const { t } = useTranslation();

  const updatePrompt = (i: number, prompt: string) =>
    onChange(
      questions.map((existing, idx) =>
        idx === i ? { ...existing, prompt } : existing,
      ),
    );

  const remove = (i: number) =>
    onChange(questions.filter((_, idx) => idx !== i));

  const move = (i: number, delta: number) => {
    const j = i + delta;
    if (j < 0 || j >= questions.length) return;
    const next = [...questions];
    [next[i], next[j]] = [next[j], next[i]];
    onChange(next);
  };

  const add = () => onChange([...questions, emptyFreeTextQuestion()]);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold">
          {t("admin.homework.manualQuestions.title")}
        </h3>
        <span className="text-xs text-muted-foreground">
          {questions.length === 1
            ? t("admin.homework.manualQuestions.countSingular", {
                count: questions.length,
              })
            : t("admin.homework.manualQuestions.countPlural", {
                count: questions.length,
              })}
        </span>
      </div>

      <p className="text-xs text-muted-foreground">
        {t("admin.homework.manualQuestions.hint")}
      </p>

      {questions.length === 0 ? (
        <p className="rounded-md border border-dashed p-4 text-sm text-muted-foreground">
          {t("admin.homework.manualQuestions.empty")}
        </p>
      ) : (
        <ul className="space-y-3">
          {questions.map((q, i) => (
            <li
              key={q.id ?? i}
              className="space-y-2 rounded-lg border bg-card p-3"
            >
              <div className="flex items-center justify-between gap-2">
                <Label htmlFor={`manual-q-${i}`}>
                  {t("admin.homework.manualQuestions.questionLabel", {
                    index: i + 1,
                  })}
                </Label>
                <div className="flex items-center gap-1">
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2 text-xs"
                    disabled={i === 0}
                    onClick={() => move(i, -1)}
                  >
                    ↑
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2 text-xs"
                    disabled={i === questions.length - 1}
                    onClick={() => move(i, 1)}
                  >
                    ↓
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-7 px-2 text-xs text-destructive"
                    onClick={() => remove(i)}
                  >
                    {t("admin.homework.manualQuestions.remove")}
                  </Button>
                </div>
              </div>
              <Input
                id={`manual-q-${i}`}
                value={q.prompt}
                onChange={(e) => updatePrompt(i, e.target.value)}
                placeholder={t(
                  "admin.homework.manualQuestions.promptPlaceholder",
                )}
                maxLength={2000}
              />
            </li>
          ))}
        </ul>
      )}

      <Button type="button" variant="outline" size="sm" onClick={add}>
        {t("admin.homework.manualQuestions.addQuestion")}
      </Button>
    </div>
  );
}
