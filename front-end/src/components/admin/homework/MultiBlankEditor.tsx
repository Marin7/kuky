import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import type { MultiBlankStructure } from "@/lib/admin";
import { countBlanks } from "@/lib/blankTokens";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface Props {
  prompt: string;
  structure: MultiBlankStructure;
  onChange: (structure: MultiBlankStructure) => void;
}

/**
 * Authoring UI for MULTI_BLANK — the passage itself is edited via the shared
 * prompt textarea in `QuestionEditorCard`; this renders one accepted-answer
 * list per `___` token found in the prompt, keeping `structure.blanks` synced
 * to the current blank count.
 */
export function MultiBlankEditor({ prompt, structure, onChange }: Props) {
  const { t } = useTranslation();
  const blankCount = countBlanks(prompt);
  const blanks = structure.blanks ?? [];

  useEffect(() => {
    if (blanks.length === blankCount) return;
    const next = Array.from(
      { length: blankCount },
      (_, i) => blanks[i] ?? { acceptedAnswers: [""] },
    );
    onChange({ blanks: next });
    // Only re-sync when the parsed blank count changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [blankCount]);

  const setAnswer = (blankIndex: number, answerIndex: number, value: string) =>
    onChange({
      blanks: blanks.map((b, i) =>
        i === blankIndex
          ? {
              acceptedAnswers: b.acceptedAnswers.map((a, j) =>
                j === answerIndex ? value : a,
              ),
            }
          : b,
      ),
    });

  const addAnswer = (blankIndex: number) =>
    onChange({
      blanks: blanks.map((b, i) =>
        i === blankIndex ? { acceptedAnswers: [...b.acceptedAnswers, ""] } : b,
      ),
    });

  const removeAnswer = (blankIndex: number, answerIndex: number) =>
    onChange({
      blanks: blanks.map((b, i) =>
        i === blankIndex
          ? {
              acceptedAnswers: b.acceptedAnswers.filter(
                (_, j) => j !== answerIndex,
              ),
            }
          : b,
      ),
    });

  return (
    <div className="space-y-3 rounded-md border border-dashed p-3">
      <p className="text-xs text-muted-foreground">
        {t("admin.homework.questions.multiBlankHint")}
      </p>
      {blankCount < 1 ? (
        <p className="text-xs text-amber-700">
          {t("admin.homework.questions.multiBlankCountWarning")}
        </p>
      ) : (
        blanks.map((blank, i) => (
          <div key={i} className="space-y-1.5 rounded-md border bg-card p-2.5">
            <Label className="text-xs">
              {t("admin.homework.questions.blankLabel", { index: i + 1 })}
            </Label>
            {blank.acceptedAnswers.map((a, j) => (
              <div key={j} className="flex items-center gap-2">
                <Input
                  value={a}
                  onChange={(e) => setAnswer(i, j, e.target.value)}
                  placeholder={t(
                    "admin.homework.questions.acceptedAnswerPlaceholder",
                  )}
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="h-8 px-2 text-xs text-destructive"
                  disabled={blank.acceptedAnswers.length === 1}
                  onClick={() => removeAnswer(i, j)}
                >
                  ✕
                </Button>
              </div>
            ))}
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="h-7 text-xs"
              onClick={() => addAnswer(i)}
            >
              {t("admin.homework.questions.addAnswer")}
            </Button>
          </div>
        ))
      )}
    </div>
  );
}
