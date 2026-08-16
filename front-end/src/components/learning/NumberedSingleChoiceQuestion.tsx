import type { StudentSingleChoiceItem } from "@/lib/learning";
import { splitPromptByNumberedMarkers } from "@/lib/singleChoiceMarkers";
import {
  questionIndexLabel,
  stripLeadingEnumeration,
} from "@/lib/questionPrompt";
import { PassageText } from "./PassageText";
import { InlineChoiceTokens } from "./InlineChoiceTokens";

interface Props {
  index: number;
  questionCount: number;
  prompt: string;
  items: StudentSingleChoiceItem[];
  selections: Record<string, string>;
  onChange: (number: number, optionId: string | null) => void;
  readOnly?: boolean;
}

/**
 * Numbered opción única take: each `(N)` in the prompt becomes a clickable
 * `(option / option)` group for that item. Clicking the selected word clears it.
 */
export function NumberedSingleChoiceQuestion({
  index,
  questionCount,
  prompt,
  items,
  selections,
  onChange,
  readOnly = false,
}: Props) {
  const byNumber = new Map(items.map((item) => [item.number, item]));
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
        if (!item || item.options.length < 2) {
          return <span key={i}>({part.number})</span>;
        }
        return (
          <InlineChoiceTokens
            key={`${part.number}-${i}`}
            tokens={item.options.map((o) => ({
              text: o.label,
              optionId: o.id,
            }))}
            selectedOptionId={selections[String(part.number)] ?? null}
            onChange={(optionId) => onChange(part.number, optionId)}
            readOnly={readOnly}
          />
        );
      })}
    </div>
  );
}
