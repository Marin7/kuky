import { Fragment } from "react";
import type { InlineChoiceMatch } from "@/lib/inlineChoice";
import { cn } from "@/lib/utils";
import { PassageText } from "./PassageText";

interface Props {
  number: number;
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
  number,
  prompt,
  match,
  selectedOptionId,
  correctOptionIds,
  correct,
  revealCorrect,
}: Props) {
  const before = prompt.slice(0, match.start);
  const after = prompt.slice(match.end);

  return (
    <div className="text-base leading-9">
      <span className="font-medium">{number}. </span>
      <PassageText text={before} />
      <span>
        (
        {match.tokens.map((token, i) => {
          const selected = token.optionId === selectedOptionId;
          const isKey = correctOptionIds.includes(token.optionId);
          const highlightCorrect =
            (selected && correct) || (!selected && isKey && revealCorrect);
          const highlightWrong = selected && !correct;

          return (
            <Fragment key={token.optionId}>
              {i > 0 && " / "}
              <span
                className={cn(
                  "mx-0.5 inline rounded px-1.5 py-0.5 align-baseline text-base font-medium",
                  highlightCorrect && "bg-green-100 text-green-700",
                  highlightWrong && "bg-red-100 text-red-700",
                )}
              >
                {token.text}
              </span>
            </Fragment>
          );
        })}
        )
      </span>
      <PassageText text={after} />
    </div>
  );
}
