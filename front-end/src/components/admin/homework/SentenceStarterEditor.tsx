import { useTranslation } from "react-i18next";
import type { SentenceStarterStructure } from "@/lib/admin";
import {
  MAX_MODEL_ANSWER_CHARS,
  MAX_STARTERS,
  MAX_STARTER_CHARS,
} from "@/lib/sentenceStarter";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface Props {
  structure: SentenceStarterStructure;
  onChange: (structure: SentenceStarterStructure) => void;
}

/**
 * SENTENCE_STARTER authoring: ordered starters, each with an optional model
 * answer shown to the student only after correction (specs/057).
 */
export function SentenceStarterEditor({ structure, onChange }: Props) {
  const { t } = useTranslation();
  const items = structure.items ?? [];

  const update = (next: SentenceStarterStructure["items"]) =>
    onChange({ items: next });

  const setItem = (
    i: number,
    patch: Partial<SentenceStarterStructure["items"][number]>,
  ) => update(items.map((it, j) => (j === i ? { ...it, ...patch } : it)));

  const move = (i: number, delta: number) => {
    const j = i + delta;
    if (j < 0 || j >= items.length) return;
    const next = [...items];
    [next[i], next[j]] = [next[j], next[i]];
    update(next);
  };

  return (
    <div className="space-y-2">
      <Label>
        {t("admin.homework.questions.sentenceStarter.startersTitle")}
      </Label>
      <p className="text-xs text-muted-foreground">
        {t("admin.homework.questions.sentenceStarter.startersHint", {
          max: MAX_STARTERS,
        })}
      </p>
      <ol className="space-y-2">
        {items.map((item, i) => (
          <li
            key={i}
            className="space-y-2 rounded-md border bg-muted/20 p-2 sm:flex sm:items-start sm:gap-2 sm:space-y-0"
          >
            <span className="hidden pt-2 text-xs text-muted-foreground sm:block">
              {i + 1}.
            </span>
            <div className="min-w-0 flex-1 space-y-2">
              <Input
                value={item.starter}
                maxLength={MAX_STARTER_CHARS}
                onChange={(e) => setItem(i, { starter: e.target.value })}
                placeholder={t(
                  "admin.homework.questions.sentenceStarter.starterPlaceholder",
                )}
                aria-invalid={!item.starter.trim()}
              />
              <Input
                value={item.modelAnswer ?? ""}
                maxLength={MAX_MODEL_ANSWER_CHARS}
                onChange={(e) => setItem(i, { modelAnswer: e.target.value })}
                placeholder={t(
                  "admin.homework.questions.sentenceStarter.modelAnswerPlaceholder",
                )}
              />
            </div>
            <div className="flex shrink-0 gap-1">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="h-8 px-2 text-xs"
                disabled={i === 0}
                onClick={() => move(i, -1)}
                aria-label={t(
                  "admin.homework.questions.sentenceStarter.moveUp",
                )}
              >
                ↑
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="h-8 px-2 text-xs"
                disabled={i === items.length - 1}
                onClick={() => move(i, 1)}
                aria-label={t(
                  "admin.homework.questions.sentenceStarter.moveDown",
                )}
              >
                ↓
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="h-8 px-2 text-xs text-destructive"
                disabled={items.length <= 1}
                onClick={() => update(items.filter((_, j) => j !== i))}
                aria-label={t(
                  "admin.homework.questions.sentenceStarter.remove",
                )}
              >
                ✕
              </Button>
            </div>
          </li>
        ))}
      </ol>
      {items.some((it) => !it.starter.trim()) && (
        <p className="text-xs text-destructive">
          {t("admin.homework.questions.sentenceStarter.emptyStarter")}
        </p>
      )}
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="h-8 text-xs"
        disabled={items.length >= MAX_STARTERS}
        onClick={() => update([...items, { starter: "" }])}
      >
        {t("admin.homework.questions.sentenceStarter.add")}
      </Button>
    </div>
  );
}
