import type { StudentSingleChoiceItem, UnitResult } from "@/lib/learning";
import { splitPromptByNumberedMarkers } from "@/lib/singleChoiceMarkers";
import {
  questionIndexLabel,
  stripLeadingEnumeration,
} from "@/lib/questionPrompt";
import { PassageText } from "./PassageText";
import { InlineChoiceTokensResult } from "./InlineChoiceTokens";

interface Props {
  index: number;
  questionCount: number;
  prompt: string;
  items: StudentSingleChoiceItem[];
  unitResults: UnitResult[];
  showAllAnswers?: boolean;
}

function normalize(value: string | null | undefined): string {
  return (value ?? "").trim().normalize("NFC").toLocaleLowerCase("es");
}

function optionIdByLabel(
  item: StudentSingleChoiceItem,
  label: string | null | undefined,
): string | null {
  const key = normalize(label);
  if (!key) return null;
  return item.options.find((o) => normalize(o.label) === key)?.id ?? null;
}

/**
 * Graded numbered opción única: `(N)` markers become `(option / option)`
 * with the pick highlighted (green/red). Missed items also mark the key.
 */
export function NumberedInlineSingleChoiceResult({
  index,
  questionCount,
  prompt,
  items,
  unitResults,
  showAllAnswers = false,
}: Props) {
  const byNumber = new Map(items.map((item) => [item.number, item]));
  const unitByNumber = new Map(
    unitResults.map((unit) => [unit.index + 1, unit]),
  );
  const parts = splitPromptByNumberedMarkers(prompt);

  return (
    <div className="text-base font-medium leading-9">
      {questionIndexLabel(index, questionCount)}
      {parts.map((part, i) => {
        if (part.type === "text") {
          const text = i === 0 ? stripLeadingEnumeration(part.text) : part.text;
          return <PassageText key={i} text={text} />;
        }
        const item = byNumber.get(part.number);
        const unit = unitByNumber.get(part.number);
        if (!item || item.options.length < 2) {
          return <span key={i}>({part.number})</span>;
        }
        const selectedId = optionIdByLabel(item, unit?.studentDisplay);
        const correctId = unit?.correct
          ? selectedId
          : optionIdByLabel(item, unit?.expectedDisplay?.[0]);
        const correct = unit?.correct ?? false;
        return (
          <InlineChoiceTokensResult
            key={`${part.number}-${i}`}
            tokens={item.options.map((o) => ({
              text: o.label,
              optionId: o.id,
            }))}
            selectedOptionId={selectedId}
            correctOptionIds={correctId ? [correctId] : []}
            correct={correct}
            revealCorrect={showAllAnswers || !correct}
          />
        );
      })}
    </div>
  );
}
