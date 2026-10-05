import { useState } from "react";
import { useTranslation } from "react-i18next";
import { questionIndexLabel } from "@/lib/questionPrompt";
import { segment } from "@/lib/spotWrongWords";
import { cn } from "@/lib/utils";
import { PassageText } from "./PassageText";

interface Props {
  index: number;
  questionCount: number;
  prompt: string;
  errorCount: number;
  selected: number[];
  onChange: (selected: number[]) => void;
  /** Teacher preview: words shown but not clickable. */
  readOnly?: boolean;
}

/**
 * SPOT_WRONG_WORDS: the passage with every word clickable. Marked words are
 * capped at the number of errors; at the cap a click on an unmarked word only
 * shows a hint. The raw prompt is segmented (no enumeration stripping) so word
 * indices match the teacher's answer key.
 */
export function SpotWrongWordsQuestion({
  index,
  questionCount,
  prompt,
  errorCount,
  selected,
  onChange,
  readOnly = false,
}: Props) {
  const { t } = useTranslation();
  const [capHint, setCapHint] = useState(false);

  const toggle = (wordIndex: number) => {
    if (selected.includes(wordIndex)) {
      setCapHint(false);
      onChange(selected.filter((i) => i !== wordIndex));
    } else if (selected.length >= errorCount) {
      setCapHint(true);
    } else {
      onChange([...selected, wordIndex].sort((a, b) => a - b));
    }
  };

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
        <p className="text-base font-medium">
          {questionIndexLabel(index, questionCount)}
          {t("learning.spotWrongWords.instruction", { count: errorCount })}
        </p>
        {!readOnly && (
          <span className="text-sm text-muted-foreground" aria-live="polite">
            {t("learning.spotWrongWords.counter", {
              marked: selected.length,
              count: errorCount,
            })}
          </span>
        )}
      </div>
      <div className="text-base leading-9">
        {segment(prompt).map((seg, i) => {
          if (seg.wordIndex === null) {
            return <PassageText key={i} text={seg.text} />;
          }
          const wordIndex = seg.wordIndex;
          const marked = selected.includes(wordIndex);
          if (readOnly) return <span key={i}>{seg.text}</span>;
          return (
            <button
              key={i}
              type="button"
              aria-pressed={marked}
              onClick={() => toggle(wordIndex)}
              className={cn(
                "-mx-0.5 inline rounded px-0.5 py-1 transition-colors focus-visible:outline-2 focus-visible:outline-ring",
                marked
                  ? "bg-destructive/15 text-destructive line-through decoration-2"
                  : "hover:bg-muted",
              )}
            >
              {seg.text}
            </button>
          );
        })}
      </div>
      {capHint && (
        <p className="text-sm text-amber-700 dark:text-amber-300" role="status">
          {t("learning.spotWrongWords.capHint")}
        </p>
      )}
    </div>
  );
}
