import { useTranslation } from "react-i18next";
import type { SpotWrongWordsStructure } from "@/lib/admin";
import { segment } from "@/lib/spotWrongWords";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PassageText } from "@/components/learning/PassageText";

const MAX_ERRORS = 30;
const MAX_CORRECTION = 100;

interface Props {
  prompt: string;
  structure: SpotWrongWordsStructure;
  onChange: (structure: SpotWrongWordsStructure) => void;
}

/**
 * Authoring for SPOT_WRONG_WORDS: the passage (typed in the card's prompt
 * field) as clickable words; clicking marks/unmarks an error, and each marked
 * word gets an optional correction shown to the student after grading.
 */
export function SpotWrongWordsEditor({ prompt, structure, onChange }: Props) {
  const { t } = useTranslation();
  const errors = structure.errors ?? [];
  const byIndex = new Map(errors.map((e) => [e.wordIndex, e]));
  const segments = segment(prompt);

  const toggle = (wordIndex: number, word: string) => {
    if (byIndex.has(wordIndex)) {
      onChange({ errors: errors.filter((e) => e.wordIndex !== wordIndex) });
    } else if (errors.length < MAX_ERRORS) {
      onChange({
        errors: [...errors, { wordIndex, word, correction: null }].sort(
          (a, b) => a.wordIndex - b.wordIndex,
        ),
      });
    }
  };

  const setCorrection = (wordIndex: number, correction: string) =>
    onChange({
      errors: errors.map((e) =>
        e.wordIndex === wordIndex ? { ...e, correction } : e,
      ),
    });

  return (
    <div className="space-y-3">
      <div className="space-y-1">
        <Label>{t("admin.homework.questions.spotWrongWords.markTitle")}</Label>
        <p className="text-xs text-muted-foreground">
          {t("admin.homework.questions.spotWrongWords.markHint", {
            max: MAX_ERRORS,
          })}
        </p>
        <div className="min-h-12 rounded-md border bg-muted/20 p-3 text-base leading-9">
          {segments.length === 0 ? (
            <span className="text-sm text-muted-foreground">
              {t("admin.homework.questions.spotWrongWords.emptyText")}
            </span>
          ) : (
            segments.map((seg, i) => {
              if (seg.wordIndex === null) {
                return <PassageText key={i} text={seg.text} />;
              }
              const wordIndex = seg.wordIndex;
              const marked = byIndex.has(wordIndex);
              return (
                <button
                  key={i}
                  type="button"
                  aria-pressed={marked}
                  onClick={() => toggle(wordIndex, seg.text)}
                  className={cn(
                    "-mx-0.5 inline rounded px-0.5 py-1 transition-colors",
                    marked
                      ? "bg-destructive/15 text-destructive line-through decoration-2"
                      : "hover:bg-muted",
                  )}
                >
                  {seg.text}
                </button>
              );
            })
          )}
        </div>
      </div>

      <div className="space-y-2">
        <Label>
          {t("admin.homework.questions.spotWrongWords.markedCount", {
            count: errors.length,
          })}
        </Label>
        {errors.length === 0 ? (
          <p className="text-xs text-muted-foreground">
            {t("admin.homework.questions.spotWrongWords.noMarks")}
          </p>
        ) : (
          <ul className="space-y-2">
            {errors.map((e) => (
              <li
                key={e.wordIndex}
                className="flex flex-col gap-1 sm:flex-row sm:items-center sm:gap-2"
              >
                <span className="min-w-28 text-sm font-medium text-destructive line-through">
                  {e.word}
                </span>
                <Input
                  value={e.correction ?? ""}
                  maxLength={MAX_CORRECTION}
                  onChange={(ev) => setCorrection(e.wordIndex, ev.target.value)}
                  placeholder={t(
                    "admin.homework.questions.spotWrongWords.correctionPlaceholder",
                  )}
                  aria-label={t(
                    "admin.homework.questions.spotWrongWords.correctionFor",
                    { word: e.word },
                  )}
                />
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
