import { useTranslation } from "react-i18next";
import type { SingleChoiceItem, SingleChoiceStructure } from "@/lib/admin";
import { parseSingleChoiceMarkers } from "@/lib/singleChoiceMarkers";
import { genId } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";

interface Props {
  prompt: string;
  structure: SingleChoiceStructure;
  onChange: (structure: SingleChoiceStructure) => void;
}

function emptyOptions() {
  return [
    { id: genId(), label: "", correct: false },
    { id: genId(), label: "", correct: false },
  ];
}

/**
 * Per-number option lists for numbered opción única. Item count follows
 * distinct `(N)` markers in the prompt (including in-progress/gapped sequences).
 */
export function SingleChoiceItemsEditor({ prompt, structure, onChange }: Props) {
  const { t } = useTranslation();
  const parsed = parseSingleChoiceMarkers(prompt);
  const numbers = parsed.numbers;
  const byNumber = new Map(
    (structure.items ?? []).map((item) => [item.number, item]),
  );
  const items: SingleChoiceItem[] = numbers.map(
    (n) => byNumber.get(n) ?? { number: n, options: emptyOptions() },
  );

  const setItem = (number: number, next: SingleChoiceItem) =>
    onChange({
      items: items.map((item) => (item.number === number ? next : item)),
    });

  const setOption = (
    number: number,
    optionIndex: number,
    label: string,
  ) => {
    const item = items.find((i) => i.number === number);
    if (!item) return;
    setItem(number, {
      ...item,
      options: item.options.map((o, i) =>
        i === optionIndex ? { ...o, label } : o,
      ),
    });
  };

  const setCorrect = (number: number, optionIndex: number) => {
    const item = items.find((i) => i.number === number);
    if (!item) return;
    setItem(number, {
      ...item,
      options: item.options.map((o, i) => ({ ...o, correct: i === optionIndex })),
    });
  };

  const addOption = (number: number) => {
    const item = items.find((i) => i.number === number);
    if (!item) return;
    setItem(number, {
      ...item,
      options: [...item.options, { id: genId(), label: "", correct: false }],
    });
  };

  const removeOption = (number: number, optionIndex: number) => {
    const item = items.find((i) => i.number === number);
    if (!item || item.options.length <= 2) return;
    setItem(number, {
      ...item,
      options: item.options.filter((_, i) => i !== optionIndex),
    });
  };

  return (
    <div className="space-y-3 rounded-md border border-dashed p-3">
      <p className="text-xs text-muted-foreground">
        {t("admin.homework.questions.singleChoiceItemsHint")}
      </p>
      {!parsed.valid && (
        <p className="text-xs text-amber-700">
          {t("admin.homework.questions.singleChoiceSequenceWarning")}
        </p>
      )}
      {items.map((item) => {
        const correctIndex = item.options.findIndex((o) => o.correct);
        return (
          <div
            key={item.number}
            className="space-y-2 rounded-md border bg-card p-2.5"
          >
            <Label className="text-xs">
              {t("admin.homework.questions.singleChoiceItemLabel", {
                number: item.number,
              })}
            </Label>
            <RadioGroup
              value={correctIndex >= 0 ? String(correctIndex) : ""}
              onValueChange={(v) => setCorrect(item.number, Number(v))}
            >
              {item.options.map((o, i) => (
                <div key={o.id ?? i} className="flex items-center gap-2">
                  <RadioGroupItem
                    value={String(i)}
                    id={`sc-item-${item.number}-o${i}`}
                  />
                  <Input
                    value={o.label}
                    onChange={(e) =>
                      setOption(item.number, i, e.target.value)
                    }
                    placeholder={t("admin.homework.questions.optionPlaceholder", {
                      index: i + 1,
                    })}
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-8 px-2 text-xs text-destructive"
                    disabled={item.options.length <= 2}
                    onClick={() => removeOption(item.number, i)}
                  >
                    ✕
                  </Button>
                </div>
              ))}
            </RadioGroup>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="h-8 text-xs"
              onClick={() => addOption(item.number)}
            >
              {t("admin.homework.questions.addOption")}
            </Button>
          </div>
        );
      })}
    </div>
  );
}
