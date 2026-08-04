import { useEffect, useMemo, useState, type DragEvent } from "react";
import { useTranslation } from "react-i18next";
import type { StudentBankItem } from "@/lib/learning";
import { countBlanks, splitPromptSegments } from "@/lib/blankTokens";
import { shuffle, cn } from "@/lib/utils";
import { PassageText } from "./PassageText";

interface Props {
  prompt: string;
  bank: StudentBankItem[];
  value: (string | null)[];
  onChange: (placements: (string | null)[]) => void;
}

/**
 * Word-bank → blanks. Bank chips are dragged (or click-selected then click a
 * blank). Visually distinct from MULTI_BLANK typed inputs.
 */
export function DragDropQuestion({ prompt, bank, value, onChange }: Props) {
  const { t } = useTranslation();
  const blankCount = Math.max(countBlanks(prompt), bank.length);
  const bankKey = bank.map((b) => b.id).join(",");
  const shuffledBank = useMemo(
    () => shuffle(bank),
    // Shuffle once per bank identity; ignore referential churn from parents.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [bankKey],
  );
  const [selected, setSelected] = useState<string | null>(null);
  const [dragOverBlank, setDragOverBlank] = useState<number | null>(null);
  const segments = splitPromptSegments(prompt);
  const placedIds = new Set(value.filter((v): v is string => v !== null));

  // Keep placements array length in sync with blank count.
  useEffect(() => {
    if (value.length === blankCount) return;
    const next = Array.from(
      { length: blankCount },
      (_, i) => value[i] ?? null,
    );
    onChange(next);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [blankCount]);

  const labelOf = (id: string | null) =>
    bank.find((b) => b.id === id)?.label ?? "";

  const ensureSize = (arr: (string | null)[]) => {
    if (arr.length >= blankCount) return [...arr];
    return Array.from({ length: blankCount }, (_, i) => arr[i] ?? null);
  };

  const place = (blankIndex: number, itemId: string) => {
    const next = ensureSize(value).map((v) => (v === itemId ? null : v));
    next[blankIndex] = itemId;
    onChange(next);
    setSelected(null);
    setDragOverBlank(null);
  };

  const clearBlank = (blankIndex: number) => {
    const next = ensureSize(value);
    next[blankIndex] = null;
    onChange(next);
  };

  const onBlankClick = (blankIndex: number) => {
    if (selected) {
      place(blankIndex, selected);
      return;
    }
    if (value[blankIndex]) clearBlank(blankIndex);
  };

  const onBankItemClick = (id: string) => {
    if (placedIds.has(id)) return;
    setSelected((prev) => (prev === id ? null : id));
  };

  const onDragStart = (e: DragEvent<HTMLButtonElement>, id: string) => {
    e.dataTransfer.setData("text/plain", id);
    e.dataTransfer.effectAllowed = "move";
    setSelected(id);
  };

  const onDrop = (e: DragEvent<HTMLButtonElement>, blankIndex: number) => {
    e.preventDefault();
    setDragOverBlank(null);
    const id = e.dataTransfer.getData("text/plain");
    if (id) place(blankIndex, id);
  };

  return (
    <div className="space-y-4">
      {/* Word bank first — the defining affordance vs typed fill-gaps */}
      <div className="rounded-lg border-2 border-dashed border-primary/40 bg-primary/5 p-3">
        <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-primary">
          {t("learning.exercisePage.wordBank")}
        </p>
        <p className="mb-3 text-xs text-muted-foreground">
          {t("learning.exercisePage.dragDropInstructions")}
        </p>
        <div className="flex min-h-10 flex-wrap gap-2">
          {shuffledBank.length === 0 ? (
            <p className="text-sm text-destructive">
              {t("learning.exercisePage.wordBankMissing")}
            </p>
          ) : (
            shuffledBank.map((item) => {
              const placed = placedIds.has(item.id);
              return (
                <button
                  key={item.id}
                  type="button"
                  draggable={!placed}
                  onDragStart={(e) => onDragStart(e, item.id)}
                  onDragEnd={() => setSelected(null)}
                  onClick={() => onBankItemClick(item.id)}
                  disabled={placed}
                  aria-pressed={selected === item.id}
                  className={cn(
                    "rounded-md border px-3 py-1.5 text-sm font-medium shadow-sm transition",
                    placed
                      ? "cursor-not-allowed border-dashed bg-muted/40 text-muted-foreground line-through opacity-40"
                      : selected === item.id
                        ? "cursor-grab border-primary bg-primary text-primary-foreground ring-2 ring-primary/30"
                        : "cursor-grab border-border bg-background hover:border-primary hover:bg-muted active:cursor-grabbing",
                  )}
                >
                  {item.label}
                </button>
              );
            })
          )}
        </div>
      </div>

      {/* Passage with drop targets — not text inputs */}
      <div className="rounded-lg border bg-card p-3">
        <p className="mb-2 text-xs font-medium text-muted-foreground">
          {t("learning.exercisePage.dropTargets")}
        </p>
        <div className="text-sm leading-10">
          {segments.map((seg, i) =>
            seg.type === "text" ? (
              <PassageText key={i} text={seg.text} />
            ) : (
              <button
                key={i}
                type="button"
                onClick={() => onBlankClick(seg.index)}
                onDragOver={(e) => {
                  e.preventDefault();
                  e.dataTransfer.dropEffect = "move";
                  setDragOverBlank(seg.index);
                }}
                onDragLeave={() =>
                  setDragOverBlank((cur) =>
                    cur === seg.index ? null : cur,
                  )
                }
                onDrop={(e) => onDrop(e, seg.index)}
                className={cn(
                  "mx-1 inline-flex h-9 min-w-28 items-center justify-center rounded-md border-2 border-dashed px-2 align-baseline text-sm transition",
                  value[seg.index]
                    ? "cursor-pointer border-solid border-primary/50 bg-primary/10 font-medium text-foreground"
                    : "bg-muted/30 text-muted-foreground",
                  dragOverBlank === seg.index &&
                    "border-primary bg-primary/20 ring-2 ring-primary/20",
                  selected &&
                    !value[seg.index] &&
                    "border-primary animate-pulse",
                )}
              >
                {value[seg.index]
                  ? labelOf(value[seg.index])
                  : t("learning.exercisePage.dropHere")}
              </button>
            ),
          )}
        </div>
        {selected && (
          <p className="mt-2 text-xs text-primary">
            {t("learning.exercisePage.selectedHint", {
              word: labelOf(selected),
            })}
          </p>
        )}
      </div>
    </div>
  );
}
