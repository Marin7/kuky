import { useTranslation } from "react-i18next";
import type { StudentSingleChoiceItem } from "@/lib/learning";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";

interface Props {
  questionId: string;
  items: StudentSingleChoiceItem[];
  selections: Record<string, string>;
  onChange: (number: number, optionId: string) => void;
}

/** Stacked radio groups — one pick-one list per numbered `(N)` item. */
export function NumberedSingleChoiceQuestion({
  questionId,
  items,
  selections,
  onChange,
}: Props) {
  const { t } = useTranslation();
  const sorted = [...items].sort((a, b) => a.number - b.number);

  return (
    <div className="space-y-4">
      {sorted.map((item) => (
        <div key={item.number} className="space-y-2">
          <p className="text-sm font-medium text-muted-foreground">
            {t("learning.numberedSingleChoice.itemLabel", {
              number: item.number,
            })}
          </p>
          <RadioGroup
            value={selections[String(item.number)] ?? ""}
            onValueChange={(v) => onChange(item.number, v)}
          >
            {item.options.map((o) => (
              <label
                key={o.id}
                className="flex items-center gap-2.5 text-base leading-snug"
              >
                <RadioGroupItem
                  value={o.id}
                  id={`${questionId}-${item.number}-${o.id}`}
                />
                {o.label}
              </label>
            ))}
          </RadioGroup>
        </div>
      ))}
    </div>
  );
}
