import type { InlineChoiceMatch } from "@/lib/inlineChoice";
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
  match: InlineChoiceMatch;
  selectedOptionId: string | null;
  correctOptionIds: string[];
  correct: boolean;
  /** When true, also mark the correct word if the student missed it. */
  revealCorrect: boolean;
}

/**
 * Graded classic opción única: highlight the chosen word in the sentence
 * (green if right, red if wrong). Wrong answers also mark the expected word.
 */
export function InlineSingleChoiceResult({
  index,
  questionCount,
  prompt,
  match,
  selectedOptionId,
  correctOptionIds,
  correct,
  revealCorrect,
}: Props) {
  const before = stripLeadingEnumeration(prompt.slice(0, match.start));
  const after = prompt.slice(match.end);

  return (
    <div className="text-base font-medium leading-9">
      {questionIndexLabel(index, questionCount)}
      <PassageText text={before} />
      <InlineChoiceTokensResult
        tokens={match.tokens}
        selectedOptionId={selectedOptionId}
        correctOptionIds={correctOptionIds}
        correct={correct}
        revealCorrect={revealCorrect}
      />
      <PassageText text={after} />
    </div>
  );
}
