import { useTranslation } from "react-i18next";
import { splitPromptSegments } from "@/lib/blankTokens";
import type { UnitResult } from "@/lib/learning";
import { PassageText } from "./PassageText";

interface Props {
  number: number;
  prompt: string;
  unitResults: UnitResult[];
}

function displayOrDash(
  value: string | null | undefined,
  empty: string,
): string {
  const trimmed = value?.trim();
  return trimmed ? trimmed : empty;
}

/** Graded MULTI_BLANK review: passage with filled blanks and per-blank feedback. */
export function MultiBlankResult({ number, prompt, unitResults }: Props) {
  const { t } = useTranslation();
  const noAnswer = t("learning.exerciseResult.noAnswer");
  const segments = splitPromptSegments(prompt);

  return (
    <div className="text-base leading-9">
      <span className="font-medium">{number}. </span>
      {segments.map((seg, i) => {
        if (seg.type === "text") {
          return <PassageText key={i} text={seg.text} />;
        }

        const unit = unitResults[seg.index];
        const correct = unit?.correct ?? false;
        const student = displayOrDash(unit?.studentDisplay, noAnswer);
        const expected =
          !correct && unit?.expectedDisplay && unit.expectedDisplay.length > 0
            ? unit.expectedDisplay.join(" / ")
            : null;

        return (
          <span
            key={i}
            className={`mx-0.5 inline-flex flex-wrap items-baseline gap-x-1 rounded px-1.5 py-0.5 align-baseline text-sm font-medium ${
              correct
                ? "bg-green-100 text-green-700"
                : "bg-red-100 text-red-700"
            }`}
          >
            <span>{student}</span>
            {expected && (
              <span className="font-normal opacity-90">
                ({t("learning.exerciseResult.unitExpected")} {expected})
              </span>
            )}
          </span>
        );
      })}
    </div>
  );
}
