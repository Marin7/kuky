import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import type { StudentMatchItem, MatchingAnswer } from "@/lib/learning";
import { shuffle, cn } from "@/lib/utils";

interface Props {
  left: StudentMatchItem[];
  right: StudentMatchItem[];
  pairs: MatchingAnswer["pairs"];
  onChange: (pairs: MatchingAnswer["pairs"]) => void;
}

const PAIR_COLORS = [
  "border-blue-500 bg-blue-50 text-blue-700",
  "border-emerald-500 bg-emerald-50 text-emerald-700",
  "border-amber-500 bg-amber-50 text-amber-700",
  "border-purple-500 bg-purple-50 text-purple-700",
  "border-pink-500 bg-pink-50 text-pink-700",
  "border-cyan-500 bg-cyan-50 text-cyan-700",
];

/**
 * Renders a MATCHING question: independently shuffled left/right lists.
 * Click-to-pair — select a left item, then its match on the right (or click
 * a paired item again to clear it). Distractors on either side stay unpaired.
 */
export function MatchingQuestion({ left, right, pairs, onChange }: Props) {
  const { t } = useTranslation();
  const shuffledLeft = useMemo(() => shuffle(left), [left]);
  const shuffledRight = useMemo(() => shuffle(right), [right]);
  const [selectedLeft, setSelectedLeft] = useState<string | null>(null);

  const pairIndex = (leftId: string) =>
    pairs.findIndex((p) => p.leftId === leftId);
  const rightPairIndex = (rightId: string) =>
    pairs.findIndex((p) => p.rightId === rightId);
  const colorFor = (idx: number) => PAIR_COLORS[idx % PAIR_COLORS.length];

  const clickLeft = (id: string) => {
    const idx = pairIndex(id);
    if (idx >= 0) {
      onChange(pairs.filter((_, i) => i !== idx));
      setSelectedLeft(null);
      return;
    }
    setSelectedLeft((prev) => (prev === id ? null : id));
  };

  const clickRight = (id: string) => {
    if (selectedLeft) {
      const withoutConflicts = pairs.filter(
        (p) => p.leftId !== selectedLeft && p.rightId !== id,
      );
      onChange([...withoutConflicts, { leftId: selectedLeft, rightId: id }]);
      setSelectedLeft(null);
      return;
    }
    const idx = rightPairIndex(id);
    if (idx >= 0) onChange(pairs.filter((_, i) => i !== idx));
  };

  return (
    <div className="space-y-2">
      <p className="text-sm text-muted-foreground">
        {t("learning.exercisePage.matchingInstructions")}
      </p>
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <p className="text-sm font-medium text-muted-foreground">
            {t("learning.exercisePage.leftColumn")}
          </p>
          {shuffledLeft.map((item) => {
            const idx = pairIndex(item.id);
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => clickLeft(item.id)}
                className={cn(
                  "block w-full rounded border px-3 py-2 text-left text-base transition",
                  idx >= 0
                    ? colorFor(idx)
                    : selectedLeft === item.id
                      ? "border-primary bg-primary/10"
                      : "hover:bg-muted",
                )}
              >
                {item.label}
              </button>
            );
          })}
        </div>
        <div className="space-y-2">
          <p className="text-sm font-medium text-muted-foreground">
            {t("learning.exercisePage.rightColumn")}
          </p>
          {shuffledRight.map((item) => {
            const idx = rightPairIndex(item.id);
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => clickRight(item.id)}
                className={cn(
                  "block w-full rounded border px-3 py-2 text-left text-base transition",
                  idx >= 0 ? colorFor(idx) : "hover:bg-muted",
                )}
              >
                {item.label}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
