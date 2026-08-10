import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import type { DragDropStructure } from "@/lib/admin";
import { countBlanks } from "@/lib/blankTokens";
import { MAX_ACCEPTED_PER_BLANK, MAX_BANK_ITEMS } from "@/lib/exerciseLimits";
import { genId } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface Props {
  prompt: string;
  structure: DragDropStructure;
  onChange: (structure: DragDropStructure) => void;
}

/**
 * Authoring UI for DRAG_DROP — free word bank (may exceed blank count for
 * alternates/distractors) plus per-blank multi-select of correct bank items.
 */
export function DragDropEditor({ prompt, structure, onChange }: Props) {
  const { t } = useTranslation();
  const blankCount = countBlanks(prompt);
  const bank = structure.bank ?? [];
  const blanks = structure.blanks ?? [];

  useEffect(() => {
    let nextBank = bank;
    let nextBlanks = blanks;
    let changed = false;

    if (nextBank.length < blankCount) {
      nextBank = [
        ...nextBank,
        ...Array.from({ length: blankCount - nextBank.length }, () => ({
          id: genId(),
          label: "",
        })),
      ];
      changed = true;
    }

    // Migrate legacy positional structure (no blanks) into canonical form.
    if (
      blankCount >= 2 &&
      nextBlanks.length === 0 &&
      nextBank.length === blankCount
    ) {
      nextBlanks = nextBank.map((item) => ({ correctBankIds: [item.id] }));
      changed = true;
    }

    if (nextBlanks.length !== blankCount && blankCount >= 2) {
      const used = new Set(nextBlanks.flatMap((b) => b.correctBankIds));
      nextBlanks = Array.from({ length: blankCount }, (_, i) => {
        if (nextBlanks[i]?.correctBankIds?.length) return nextBlanks[i];
        const free = nextBank.find((item) => !used.has(item.id));
        if (free) {
          used.add(free.id);
          return { correctBankIds: [free.id] };
        }
        const created = { id: genId(), label: "" };
        nextBank = [...nextBank, created];
        used.add(created.id);
        return { correctBankIds: [created.id] };
      });
      changed = true;
    }

    if (changed) {
      onChange({ bank: nextBank, blanks: nextBlanks });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [blankCount]);

  const setLabel = (id: string, label: string) =>
    onChange({
      bank: bank.map((item) => (item.id === id ? { ...item, label } : item)),
      blanks,
    });

  const addBankItem = () => {
    if (bank.length >= MAX_BANK_ITEMS) return;
    onChange({
      bank: [...bank, { id: genId(), label: "" }],
      blanks,
    });
  };

  const removeBankItem = (id: string) => {
    if (bank.length <= blankCount) return;
    onChange({
      bank: bank.filter((item) => item.id !== id),
      blanks: blanks.map((b) => ({
        correctBankIds: b.correctBankIds.filter((cid) => cid !== id),
      })),
    });
  };

  const toggleCorrect = (blankIndex: number, bankId: string) => {
    onChange({
      bank,
      blanks: blanks.map((b, i) => {
        if (i !== blankIndex) return b;
        const has = b.correctBankIds.includes(bankId);
        if (has) {
          if (b.correctBankIds.length <= 1) return b;
          return {
            correctBankIds: b.correctBankIds.filter((id) => id !== bankId),
          };
        }
        if (b.correctBankIds.length >= MAX_ACCEPTED_PER_BLANK) return b;
        return { correctBankIds: [...b.correctBankIds, bankId] };
      }),
    });
  };

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
        <div className="space-y-4">
          <div className="space-y-2">
            {bank.map((item, i) => (
              <div key={item.id} className="flex items-center gap-2">
                <Label className="w-28 shrink-0 text-xs">
                  {t("admin.homework.questions.bankItemLabel", {
                    index: i + 1,
                  })}
                </Label>
                <Input
                  value={item.label}
                  onChange={(e) => setLabel(item.id, e.target.value)}
                  placeholder={t(
                    "admin.homework.questions.bankItemPlaceholder",
                  )}
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="h-8 px-2 text-xs text-destructive"
                  disabled={bank.length <= blankCount}
                  onClick={() => removeBankItem(item.id)}
                  title={t("admin.homework.questions.removeBankItem")}
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
              disabled={bank.length >= MAX_BANK_ITEMS}
              onClick={addBankItem}
            >
              {t("admin.homework.questions.addBankItem")}
            </Button>
            {bank.length >= MAX_BANK_ITEMS && (
              <p className="text-[11px] text-muted-foreground">
                {t("admin.homework.questions.maxBankItems", {
                  max: MAX_BANK_ITEMS,
                })}
              </p>
            )}
          </div>

          <div className="space-y-2 border-t pt-3">
            <p className="text-xs font-medium text-foreground">
              {t("admin.homework.questions.correctBankPerBlank")}
            </p>
            <p className="text-[11px] text-muted-foreground">
              {t("admin.homework.questions.correctBankPerBlankHint")}
            </p>
            {blanks.map((blank, blankIndex) => (
              <div
                key={blankIndex}
                className="space-y-1.5 rounded-md border bg-background/80 p-2.5"
              >
                <Label className="text-xs">
                  {t("admin.homework.questions.blankLabel", {
                    index: blankIndex + 1,
                  })}
                </Label>
                <div className="flex flex-wrap gap-2">
                  {bank.map((item) => {
                    const checked = blank.correctBankIds.includes(item.id);
                    const atMax =
                      !checked &&
                      blank.correctBankIds.length >= MAX_ACCEPTED_PER_BLANK;
                    const disabled =
                      atMax || (checked && blank.correctBankIds.length === 1);
                    return (
                      <label
                        key={item.id}
                        className={`inline-flex items-center gap-1.5 rounded-md border px-2 py-1 text-xs ${
                          checked
                            ? "border-primary bg-primary/10"
                            : "border-border bg-background"
                        } ${atMax ? "opacity-50" : ""}`}
                      >
                        <input
                          type="checkbox"
                          className="accent-primary"
                          checked={checked}
                          disabled={disabled}
                          onChange={() => toggleCorrect(blankIndex, item.id)}
                        />
                        <span>
                          {item.label.trim() ||
                            t("admin.homework.questions.bankItemUntitled", {
                              index:
                                bank.findIndex((b) => b.id === item.id) + 1,
                            })}
                        </span>
                      </label>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
