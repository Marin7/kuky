import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import type { StudentMistakeEntry } from "@/lib/admin";
import type { UnitResult } from "@/lib/learning";
import { splitPromptSegments } from "@/lib/blankTokens";
import { splitPromptByNumberedMarkers } from "@/lib/singleChoiceMarkers";
import { stripLeadingEnumeration } from "@/lib/questionPrompt";
import { QuestionResultBlock } from "@/components/learning/ExerciseResult";
import { QuestionHeading } from "@/components/learning/QuestionHeading";
import { SpotWrongWordsResult } from "@/components/learning/SpotWrongWordsResult";
import { TextWithLinks } from "@/components/learning/TextWithLinks";

/** Prompt text interleaved with answer slots (blanks, `(N)` markers). */
type Part = { type: "text"; text: string } | { type: "slot"; unit: number };

const SENTENCE_END = /[.!?\n]/;

/**
 * The sentence around slot `at`: text back to the previous sentence end and
 * forward to the next one. Other slots inside it are kept as placeholders.
 */
function sentenceAround(parts: Part[], at: number): Part[] {
  const before: Part[] = [];
  for (let i = at - 1; i >= 0; i--) {
    const part = parts[i];
    if (part.type === "slot") {
      before.unshift(part);
      continue;
    }
    let cut = part.text.length - 1;
    while (cut >= 0 && !SENTENCE_END.test(part.text[cut])) cut--;
    before.unshift({ type: "text", text: part.text.slice(cut + 1) });
    if (cut >= 0) break;
  }
  const after: Part[] = [];
  for (let i = at + 1; i < parts.length; i++) {
    const part = parts[i];
    if (part.type === "slot") {
      after.push(part);
      continue;
    }
    const end = part.text.search(SENTENCE_END);
    if (end < 0) {
      after.push(part);
      continue;
    }
    const ch = part.text[end];
    after.push({
      type: "text",
      text: part.text.slice(0, ch === "\n" ? end : end + 1),
    });
    break;
  }
  return [...before, parts[at], ...after];
}

function display(value: string | null | undefined, empty: string): string {
  const trimmed = value?.trim();
  return trimmed ? trimmed : empty;
}

function WrongSlot({ unit }: { unit: UnitResult | undefined }) {
  const { t } = useTranslation();
  const expected = unit?.expectedDisplay?.length
    ? unit.expectedDisplay.join(" / ")
    : null;
  return (
    <span className="mx-0.5 inline-flex flex-wrap items-baseline gap-x-1 rounded bg-red-100 px-1.5 py-0.5 align-baseline text-sm font-medium text-red-700">
      <span>
        {display(unit?.studentDisplay, t("learning.exerciseResult.noAnswer"))}
      </span>
      {expected && (
        <span className="font-normal opacity-90">
          ({t("learning.exerciseResult.unitExpectedSingle")} {expected})
        </span>
      )}
    </span>
  );
}

/** One line per wrong slot: its sentence, other slots blanked out. */
function WrongSlotSentences({
  parts,
  unitFor,
}: {
  parts: Part[];
  unitFor: (unit: number) => UnitResult | undefined;
}) {
  const lines: ReactNode[] = [];
  parts.forEach((part, at) => {
    if (part.type !== "slot" || unitFor(part.unit)?.correct) return;
    lines.push(
      <li key={at} className="leading-8">
        {sentenceAround(parts, at).map((p, i) =>
          p.type === "text" ? (
            <TextWithLinks key={i} text={p.text} />
          ) : p === part ? (
            <WrongSlot key={i} unit={unitFor(p.unit)} />
          ) : (
            <span key={i} className="mx-0.5 text-muted-foreground">
              ___
            </span>
          ),
        )}
      </li>,
    );
  });
  return (
    <ul className="list-disc space-y-1 pl-5 text-base font-medium">{lines}</ul>
  );
}

/**
 * A mistake entry's answer detail showing only what the student got wrong:
 * wrong blanks / numbered items (with their sentence), wrong table cells, wrong
 * matching pairs, the inline passage for spot-the-wrong-words. Other kinds are
 * a single answer and use the standard block.
 */
export function MistakeOnlyDetail({ entry }: { entry: StudentMistakeEntry }) {
  const { t } = useTranslation();
  const { question, result } = entry;
  const units = result.unitResults ?? [];

  if (question.kind === "MULTI_BLANK" || question.kind === "DRAG_DROP") {
    const parts: Part[] = splitPromptSegments(
      stripLeadingEnumeration(question.prompt),
    ).map((s) =>
      s.type === "text"
        ? { type: "text", text: s.text }
        : { type: "slot", unit: s.index },
    );
    return <WrongSlotSentences parts={parts} unitFor={(i) => units[i]} />;
  }

  if (question.kind === "SPOT_WRONG_WORDS") {
    return (
      <SpotWrongWordsResult
        index={1}
        questionCount={1}
        prompt={question.prompt}
        unitResults={units}
      />
    );
  }

  if (
    question.kind === "SINGLE_CHOICE" &&
    (question.structure?.items?.length ?? 0) > 0
  ) {
    const byNumber = new Map(units.map((u) => [u.index + 1, u]));
    const parts: Part[] = splitPromptByNumberedMarkers(
      stripLeadingEnumeration(question.prompt),
    ).map((p) =>
      p.type === "text"
        ? { type: "text", text: p.text }
        : { type: "slot", unit: p.number },
    );
    return (
      <WrongSlotSentences parts={parts} unitFor={(n) => byNumber.get(n)} />
    );
  }

  if (question.kind === "TABLE_FILL" && question.structure) {
    const rowHeaders = question.structure.rowHeaders ?? [];
    const colHeaders = question.structure.colHeaders ?? [];
    const blanks = (question.structure.cells ?? [])
      .filter((c) => c.type === "blank")
      .sort((a, b) => a.r - b.r || a.c - b.c);
    return (
      <div className="space-y-2">
        <QuestionHeading index={1} questionCount={1} prompt={question.prompt} />
        <ul className="space-y-1.5">
          {blanks.map((cell, i) => {
            const unit = units[i];
            if (unit?.correct) return null;
            const where = [rowHeaders[cell.r], colHeaders[cell.c]]
              .filter(Boolean)
              .join(" · ");
            return (
              <li
                key={`${cell.r},${cell.c}`}
                className="rounded bg-red-100 px-2 py-1.5 text-xs text-red-700"
              >
                <span className="font-medium">{where}</span>
                <span className="mt-0.5 block text-[11px] opacity-90">
                  {t("learning.exerciseResult.yourAnswer")}{" "}
                  {display(
                    unit?.studentDisplay,
                    t("learning.exerciseResult.noAnswer"),
                  )}
                  {unit?.expectedDisplay?.length ? (
                    <>
                      {" · "}
                      {t("learning.exerciseResult.unitExpectedSingle")}{" "}
                      {unit.expectedDisplay.join(" / ")}
                    </>
                  ) : null}
                </span>
              </li>
            );
          })}
        </ul>
      </div>
    );
  }

  // Matching renders one row per pair (each keeping its number): drop the right
  // ones. Choice / true-false are a single answer already.
  const onlyWrong =
    question.kind === "MATCHING"
      ? { ...result, unitResults: units.filter((u) => !u.correct) }
      : result;
  return (
    <QuestionResultBlock
      question={question}
      result={onlyWrong}
      number={entry.questionPosition}
      questionCount={1}
      showAllAnswers
    />
  );
}
