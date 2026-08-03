import { useTranslation } from "react-i18next";
import type { MatchingStructure } from "@/lib/admin";
import { genId } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const NO_PAIR = "__none__";
const MAX_ITEMS = 20;

interface Props {
  structure: MatchingStructure;
  onChange: (structure: MatchingStructure) => void;
}

/**
 * Authoring UI for MATCHING — independent left/right lists (unequal length
 * and distractors allowed) plus a per-left-item picker of its correct match.
 */
export function MatchingEditor({ structure, onChange }: Props) {
  const { t } = useTranslation();
  const left = structure.left ?? [];
  const right = structure.right ?? [];
  const pairs = structure.pairs ?? [];

  const pairForLeft = (leftId: string) =>
    pairs.find((p) => p.leftId === leftId)?.rightId ?? "";

  const setPair = (leftId: string, rightId: string) => {
    const withoutLeft = pairs.filter((p) => p.leftId !== leftId);
    const withoutRight = rightId
      ? withoutLeft.filter((p) => p.rightId !== rightId)
      : withoutLeft;
    const next = rightId
      ? [...withoutRight, { leftId, rightId }]
      : withoutRight;
    onChange({ ...structure, pairs: next });
  };

  const addLeft = () =>
    onChange({ ...structure, left: [...left, { id: genId(), label: "" }] });

  const addRight = () =>
    onChange({ ...structure, right: [...right, { id: genId(), label: "" }] });

  const setLeftLabel = (id: string, label: string) =>
    onChange({
      ...structure,
      left: left.map((it) => (it.id === id ? { ...it, label } : it)),
    });

  const setRightLabel = (id: string, label: string) =>
    onChange({
      ...structure,
      right: right.map((it) => (it.id === id ? { ...it, label } : it)),
    });

  const removeLeft = (id: string) =>
    onChange({
      ...structure,
      left: left.filter((it) => it.id !== id),
      pairs: pairs.filter((p) => p.leftId !== id),
    });

  const removeRight = (id: string) =>
    onChange({
      ...structure,
      right: right.filter((it) => it.id !== id),
      pairs: pairs.filter((p) => p.rightId !== id),
    });

  return (
    <div className="space-y-3 rounded-md border border-dashed p-3">
      <p className="text-xs text-muted-foreground">
        {t("admin.homework.questions.matchingHint")}
      </p>
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label className="text-xs">
            {t("admin.homework.questions.leftColumn")}
          </Label>
          {left.map((item) => (
            <div key={item.id} className="flex items-center gap-2">
              <Input
                value={item.label}
                onChange={(e) => setLeftLabel(item.id, e.target.value)}
                placeholder={t("admin.homework.questions.itemPlaceholder")}
              />
              <Select
                value={pairForLeft(item.id) || NO_PAIR}
                onValueChange={(v) => setPair(item.id, v === NO_PAIR ? "" : v)}
              >
                <SelectTrigger className="h-9 w-40 shrink-0 text-xs">
                  <SelectValue
                    placeholder={t("admin.homework.questions.matchWith")}
                  />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={NO_PAIR}>
                    {t("admin.homework.questions.noPair")}
                  </SelectItem>
                  {right.map((r) => (
                    <SelectItem key={r.id} value={r.id}>
                      {r.label || t("admin.homework.questions.itemPlaceholder")}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="h-8 px-2 text-xs text-destructive"
                disabled={left.length <= 1}
                onClick={() => removeLeft(item.id)}
              >
                ✕
              </Button>
            </div>
          ))}
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="h-8 text-xs"
            disabled={left.length >= MAX_ITEMS}
            onClick={addLeft}
          >
            {t("admin.homework.questions.addItem")}
          </Button>
        </div>
        <div className="space-y-2">
          <Label className="text-xs">
            {t("admin.homework.questions.rightColumn")}
          </Label>
          {right.map((item) => (
            <div key={item.id} className="flex items-center gap-2">
              <Input
                value={item.label}
                onChange={(e) => setRightLabel(item.id, e.target.value)}
                placeholder={t("admin.homework.questions.itemPlaceholder")}
              />
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="h-8 px-2 text-xs text-destructive"
                disabled={right.length <= 1}
                onClick={() => removeRight(item.id)}
              >
                ✕
              </Button>
            </div>
          ))}
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="h-8 text-xs"
            disabled={right.length >= MAX_ITEMS}
            onClick={addRight}
          >
            {t("admin.homework.questions.addItem")}
          </Button>
        </div>
      </div>
    </div>
  );
}
