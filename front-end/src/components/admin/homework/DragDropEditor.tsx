import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import type { DragDropStructure } from "@/lib/admin";
import { countBlanks } from "@/lib/blankTokens";
import { genId } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface Props {
  prompt: string;
  structure: DragDropStructure;
  onChange: (structure: DragDropStructure) => void;
}

/**
 * Authoring UI for DRAG_DROP — defines the word bank. Item *i* is the correct
 * word for blank *i* in the passage (students see the bank shuffled).
 */
export function DragDropEditor({ prompt, structure, onChange }: Props) {
  const { t } = useTranslation();
  const blankCount = countBlanks(prompt);
  const bank = structure.bank ?? [];

  useEffect(() => {
    if (bank.length === blankCount) return;
    const next = Array.from(
      { length: blankCount },
      (_, i) => bank[i] ?? { id: genId(), label: "" },
    );
    onChange({ bank: next });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [blankCount]);

  const setLabel = (i: number, label: string) =>
    onChange({
      bank: bank.map((item, idx) => (idx === i ? { ...item, label } : item)),
    });

  return (
    <div className="space-y-3 rounded-md border-2 border-dashed border-primary/30 bg-primary/5 p-3">
      <div>
        <p className="text-sm font-semibold text-foreground">
          {t("admin.homework.questions.wordBankTitle")}
        </p>
        <p className="text-xs text-muted-foreground">
          {t("admin.homework.questions.dragDropHint")}
        </p>
      </div>
      {blankCount < 2 ? (
        <p className="text-xs text-amber-700">
          {t("admin.homework.questions.blankCountWarning")}
        </p>
      ) : (
        <div className="space-y-2">
          {bank.map((item, i) => (
            <div key={item.id} className="flex items-center gap-2">
              <Label className="w-32 shrink-0 text-xs">
                {t("admin.homework.questions.bankWordLabel", { index: i + 1 })}
              </Label>
              <Input
                value={item.label}
                onChange={(e) => setLabel(i, e.target.value)}
                placeholder={t("admin.homework.questions.bankItemPlaceholder")}
              />
            </div>
          ))}
          {bank.some((b) => b.label.trim()) && (
            <div className="flex flex-wrap gap-2 border-t pt-2">
              <span className="w-full text-xs text-muted-foreground">
                {t("admin.homework.questions.bankPreview")}
              </span>
              {bank
                .filter((b) => b.label.trim())
                .map((b) => (
                  <span
                    key={b.id}
                    className="rounded-md border bg-background px-2 py-1 text-xs font-medium"
                  >
                    {b.label}
                  </span>
                ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
