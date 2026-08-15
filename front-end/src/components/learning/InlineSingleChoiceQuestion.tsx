import { Fragment } from "react";
import { useTranslation } from "react-i18next";
import type { InlineChoiceMatch } from "@/lib/inlineChoice";
import { cn } from "@/lib/utils";
import { PassageText } from "./PassageText";

interface Props {
  number: number;
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
  number,
  prompt,
  match,
  selectedOptionId,
  onChange,
}: Props) {
  const { t } = useTranslation();
  const before = prompt.slice(0, match.start);
  const after = prompt.slice(match.end);

  return (
    <div className="text-base leading-9">
      <span className="font-medium">{number}. </span>
      <PassageText text={before} />
      <span
        role="group"
        aria-label={t("learning.inlineSingleChoice.groupLabel")}
      >
        (
        {match.tokens.map((token, i) => {
          const selected = selectedOptionId === token.optionId;
          return (
            <Fragment key={token.optionId}>
              {i > 0 && " / "}
              <button
                type="button"
                aria-pressed={selected}
                onClick={() => onChange(selected ? null : token.optionId)}
                className={cn(
                  "mx-0.5 inline cursor-pointer rounded px-1.5 py-0.5 align-baseline text-base font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
                  selected
                    ? "bg-green-200 text-green-900 ring-1 ring-green-400"
                    : "bg-muted/70 text-foreground hover:bg-muted",
                )}
              >
                {token.text}
              </button>
            </Fragment>
          );
        })}
        )
      </span>
      <PassageText text={after} />
    </div>
  );
}
