import type { InlineChoiceMatch } from "@/lib/inlineChoice";
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
  match: InlineChoiceMatch;
  selectedOptionId: string | null;
  onChange: (optionId: string | null) => void;
}

/**
 * Classic opción única take: click a word inside `(option / option)` instead
 * of a radio list. Clicking the selected word deselects it.
 */
export function InlineSingleChoiceQuestion({
  index,
  questionCount,
  prompt,
  match,
  selectedOptionId,
  onChange,
}: Props) {
  // Match indices stay on the stored prompt; strip only the visible prefix so
  // worksheet copy like `1. (Soy / Estoy)…` does not become `1. 1. (Soy…`.
  const before = stripLeadingEnumeration(prompt.slice(0, match.start));
  const after = prompt.slice(match.end);

  return (
    <div className="text-base font-medium leading-9">
      {questionIndexLabel(index, questionCount)}
      <PassageText text={before} />
      <InlineChoiceTokens
        tokens={match.tokens}
        selectedOptionId={selectedOptionId}
        onChange={onChange}
      />
      <PassageText text={after} />
    </div>
  );
}
