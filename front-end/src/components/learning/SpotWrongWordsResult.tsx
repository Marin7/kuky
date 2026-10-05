import { Check, CircleAlert, X } from "lucide-react";
import { useTranslation } from "react-i18next";
import type { UnitResult } from "@/lib/learning";
import { questionIndexLabel } from "@/lib/questionPrompt";
import { segment } from "@/lib/spotWrongWords";
import { PassageText } from "./PassageText";

interface Props {
  index: number;
  questionCount: number;
  prompt: string;
  unitResults: UnitResult[];
}

type WordState = "found" | "missed" | "extra";

function stateOf(u: UnitResult): WordState {
  if (u.label === "EXTRA") return "extra";
  return u.correct ? "found" : "missed";
}

const STYLES: Record<WordState, string> = {
  found: "bg-green-100 text-green-800 dark:bg-green-950/50 dark:text-green-200",
  missed:
    "bg-red-100 text-red-800 underline decoration-wavy decoration-2 underline-offset-4 dark:bg-red-950/50 dark:text-red-200",
  extra:
    "outline outline-1 outline-dashed outline-amber-500 text-amber-800 dark:text-amber-200",
};

function StateIcon({ state }: { state: WordState }) {
  const cls = "inline size-3.5 align-[-2px]";
  if (state === "found") return <Check className={cls} aria-hidden />;
  if (state === "missed") return <X className={cls} aria-hidden />;
  return <CircleAlert className={cls} aria-hidden />;
}

/**
 * SPOT_WRONG_WORDS result inline in the passage: found and missed errors
 * (each followed by the teacher's correction) and wrongly marked words, told
 * apart by icon and underline/outline as well as colour.
 */
export function SpotWrongWordsResult({
  index,
  questionCount,
  prompt,
  unitResults,
}: Props) {
  const { t } = useTranslation();
  const byWord = new Map(unitResults.map((u) => [u.index, u]));
  const label = (state: WordState) => t(`learning.spotWrongWords.${state}`);

  return (
    <div className="space-y-2">
      <div className="text-base leading-9">
        {questionIndexLabel(index, questionCount)}
        {segment(prompt).map((seg, i) => {
          const unit =
            seg.wordIndex === null ? undefined : byWord.get(seg.wordIndex);
          if (!unit) return <PassageText key={i} text={seg.text} />;
          const state = stateOf(unit);
          const correction = unit.expectedDisplay?.[0];
          return (
            <span key={i}>
              <span
                className={`rounded px-1 py-0.5 ${STYLES[state]}`}
                title={label(state)}
              >
                <StateIcon state={state} /> {seg.text}
                <span className="sr-only"> ({label(state)})</span>
              </span>
              {correction && state !== "extra" && (
                <span className="ml-1 font-medium text-green-700 dark:text-green-300">
                  → {correction}
                </span>
              )}
            </span>
          );
        })}
      </div>
      <ul className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
        {(["found", "missed", "extra"] as const).map((state) => (
          <li key={state} className="flex items-center gap-1">
            <span className={`rounded px-1 ${STYLES[state]}`}>
              <StateIcon state={state} />
            </span>
            {label(state)}
          </li>
        ))}
      </ul>
    </div>
  );
}
