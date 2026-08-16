import { Fragment } from "react";
import { useTranslation } from "react-i18next";
import { cn } from "@/lib/utils";

export interface ChoiceToken {
  text: string;
  optionId: string;
}

const TOKEN =
  "inline rounded px-0.5 py-0.5 align-baseline text-base font-medium";

/** Clickable `(a / b)` group. Clicking the selected word clears it. */
export function InlineChoiceTokens({
  tokens,
  selectedOptionId,
  onChange,
  readOnly = false,
}: {
  tokens: ChoiceToken[];
  selectedOptionId: string | null;
  onChange: (optionId: string | null) => void;
  readOnly?: boolean;
}) {
  const { t } = useTranslation();
  return (
    <span role="group" aria-label={t("learning.inlineSingleChoice.groupLabel")}>
      (
      {tokens.map((token, i) => {
        const selected = selectedOptionId === token.optionId;
        return (
          <Fragment key={token.optionId}>
            {i > 0 && " / "}
            {readOnly ? (
              <span
                className={cn(
                  TOKEN,
                  "text-foreground underline decoration-dotted decoration-muted-foreground underline-offset-4",
                )}
              >
                {token.text}
              </span>
            ) : (
              <button
                type="button"
                aria-pressed={selected}
                onClick={() => onChange(selected ? null : token.optionId)}
                className={cn(
                  TOKEN,
                  "cursor-pointer transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
                  selected
                    ? "bg-green-200 text-green-900 hover:bg-green-300"
                    : "bg-transparent text-foreground underline decoration-dotted decoration-muted-foreground underline-offset-4 hover:bg-green-100 hover:text-green-900 hover:no-underline",
                )}
              >
                {token.text}
              </button>
            )}
          </Fragment>
        );
      })}
      )
    </span>
  );
}

/** Graded `(a / b)`: right pick green; revealed key red; wrong pick struck through. */
export function InlineChoiceTokensResult({
  tokens,
  selectedOptionId,
  correctOptionIds,
  correct,
  revealCorrect,
}: {
  tokens: ChoiceToken[];
  selectedOptionId: string | null;
  correctOptionIds: string[];
  correct: boolean;
  revealCorrect: boolean;
}) {
  return (
    <span>
      (
      {tokens.map((token, i) => {
        const selected = token.optionId === selectedOptionId;
        const isKey = correctOptionIds.includes(token.optionId);
        const isRightPick = selected && correct;
        const isWrongPick = selected && !correct;
        const isRevealedKey = !selected && isKey && revealCorrect;
        return (
          <Fragment key={token.optionId}>
            {i > 0 && " / "}
            <span
              className={cn(
                TOKEN,
                isRightPick && "bg-green-200 text-green-900",
                isRevealedKey && "bg-red-200 text-red-900",
                isWrongPick && "line-through",
              )}
            >
              {token.text}
            </span>
          </Fragment>
        );
      })}
      )
    </span>
  );
}
