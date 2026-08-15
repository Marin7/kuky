import { useTranslation } from "react-i18next";
import type {
  ExerciseResult,
  QuestionResult,
  StudentQuestion,
} from "@/lib/learning";
import {
  matchClassicInlineSingleChoice,
  classicSingleChoiceResultPrompt,
} from "@/lib/inlineChoice";
import { MultiBlankResult } from "./MultiBlankResult";
import { InlineSingleChoiceResult } from "./InlineSingleChoiceResult";
import { TableFillResult } from "./TableFillResult";

interface Props {
  questions: StudentQuestion[];
  result: ExerciseResult;
  /** When true (teacher view), always show the student's answer, not only on mistakes. */
  showAllAnswers?: boolean;
  /** Optional plain-text teacher comment (shown only when non-empty). */
  teacherFeedback?: string | null;
  /**
   * Source text / instructions the questions are based on (e.g. reading
   * passage for Lectura true/false). Shown above the score when non-empty.
   */
  instructions?: string | null;
  /** Hide the overall % / fully-correct summary (quiz destreza scores replace it). */
  hideScoreSummary?: boolean;
}

function optionLabels(
  question: StudentQuestion,
  ids: string[],
  localizeTrueFalse?: (label: string) => string,
): string {
  return question.options
    .filter((o) => ids.includes(o.id))
    .map((o) =>
      question.kind === "TRUE_FALSE" && localizeTrueFalse
        ? localizeTrueFalse(o.label)
        : o.label,
    )
    .join(", ");
}

function displayOrDash(
  value: string | null | undefined,
  empty: string,
): string {
  const trimmed = value?.trim();
  return trimmed ? trimmed : empty;
}

export function QuestionResultBlock({
  question,
  result: qr,
  number,
  showAllAnswers = false,
}: {
  question: StudentQuestion | undefined;
  result: QuestionResult;
  number: number;
  showAllAnswers?: boolean;
}) {
  const { t } = useTranslation();
  const noAnswer = t("learning.exerciseResult.noAnswer");
  const localizeTrueFalse = (label: string) =>
    t(
      label === "false"
        ? "learning.trueFalse.false"
        : "learning.trueFalse.true",
    );

  const partial = qr.score > 0 && qr.score < 1;
  const badge = qr.correct
    ? {
        text: t("learning.exerciseResult.questionCorrect"),
        cls: "bg-green-100 text-green-700",
      }
    : partial
      ? {
          text: `${t("learning.exerciseResult.questionPartial")} — ${Math.round(qr.score * 100)}%`,
          cls: "bg-amber-100 text-amber-700",
        }
      : {
          text: t("learning.exerciseResult.questionIncorrect"),
          cls: "bg-red-100 text-red-700",
        };

  const correctText =
    qr.acceptedAnswers.length > 0
      ? qr.acceptedAnswers.join(" / ")
      : question
        ? optionLabels(question, qr.correctOptionIds, localizeTrueFalse)
        : "";
  const unitResults = qr.unitResults ?? [];
  const studentChoiceText =
    question && (qr.selectedOptionIds?.length ?? 0) > 0
      ? optionLabels(question, qr.selectedOptionIds ?? [], localizeTrueFalse)
      : "";
  const hasStudentChoice = studentChoiceText.length > 0;
  const isTrueFalse = question?.kind === "TRUE_FALSE";
  const isBlankPassage =
    question?.kind === "MULTI_BLANK" || question?.kind === "DRAG_DROP";
  const inlineChoice = question
    ? matchClassicInlineSingleChoice(question)
    : null;
  const filledChoicePrompt = question
    ? classicSingleChoiceResultPrompt(question)
    : null;
  const showChoiceDetail =
    showAllAnswers || !qr.correct || partial || isTrueFalse;

  return (
    <div className="text-base">
      <div className="flex items-start justify-between gap-3">
        {isBlankPassage && question ? (
          <MultiBlankResult
            number={number}
            prompt={question.prompt}
            unitResults={unitResults}
          />
        ) : inlineChoice && question ? (
          <InlineSingleChoiceResult
            number={number}
            prompt={question.prompt}
            match={inlineChoice}
            selectedOptionId={qr.selectedOptionIds?.[0] ?? null}
            correctOptionIds={qr.correctOptionIds}
            correct={qr.correct}
            revealCorrect={showAllAnswers || !qr.correct}
          />
        ) : filledChoicePrompt && question ? (
          <MultiBlankResult
            number={number}
            prompt={filledChoicePrompt}
            unitResults={[
              {
                index: 0,
                score: qr.correct ? 1 : 0,
                correct: qr.correct,
                studentDisplay: studentChoiceText || null,
                expectedDisplay:
                  !qr.correct && correctText ? [correctText] : [],
              },
            ]}
          />
        ) : (
          <p className="whitespace-pre-wrap font-medium leading-relaxed text-foreground">
            {number}. {question?.prompt}
          </p>
        )}
        <span
          className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${badge.cls}`}
        >
          {badge.text}
        </span>
      </div>
      {unitResults.length > 0 &&
      question?.kind === "TABLE_FILL" &&
      question.structure ? (
        <TableFillResult
          structure={question.structure}
          unitResults={unitResults}
        />
      ) : unitResults.length > 0 && !isBlankPassage ? (
        <div className="mt-2 space-y-1.5">
          {unitResults.map((u) => (
            <div
              key={u.index}
              className={`rounded px-2 py-1.5 text-xs ${
                u.correct
                  ? "bg-green-100 text-green-700"
                  : "bg-red-100 text-red-700"
              }`}
            >
              <span className="font-medium">
                {u.index + 1}.{" "}
                {u.correct
                  ? t("learning.exerciseResult.unitCorrect")
                  : t("learning.exerciseResult.unitIncorrect")}
              </span>
              {(showAllAnswers || !u.correct) && (
                <span className="mt-0.5 block text-[11px] opacity-90">
                  {t("learning.exerciseResult.yourAnswer")}{" "}
                  {displayOrDash(u.studentDisplay, noAnswer)}
                  {u.expectedDisplay && u.expectedDisplay.length > 0 && (
                    <>
                      {" · "}
                      {t("learning.exerciseResult.unitExpected")}{" "}
                      {u.expectedDisplay.join(" / ")}
                    </>
                  )}
                </span>
              )}
            </div>
          ))}
        </div>
      ) : (
        !isBlankPassage &&
        !inlineChoice &&
        !filledChoicePrompt &&
        showChoiceDetail && (
          <div className="mt-2 space-y-1 text-muted-foreground">
            <p>
              <span className="font-medium text-foreground">
                {t("learning.exerciseResult.yourAnswer")}{" "}
              </span>
              {hasStudentChoice ? studentChoiceText : noAnswer}
            </p>
            {!isTrueFalse && (!qr.correct || partial) && correctText && (
              <p>
                <span className="font-medium text-foreground">
                  {t("learning.exerciseResult.correctAnswer")}{" "}
                </span>
                {correctText}
              </p>
            )}
          </div>
        )
      )}
    </div>
  );
}

export function ExerciseResult({
  questions,
  result,
  showAllAnswers = false,
  teacherFeedback = null,
  instructions = null,
  hideScoreSummary = false,
}: Props) {
  const { t } = useTranslation();
  const byId = new Map(questions.map((q) => [q.id, q]));
  const feedbackText = teacherFeedback?.trim() || null;
  const passageText = instructions?.trim() || null;
  const groups: {
    skill: string | undefined;
    items: { qr: QuestionResult; index: number }[];
  }[] = [];
  result.questions.forEach((qr, index) => {
    const skill = byId.get(qr.questionId)?.skill;
    const last = groups[groups.length - 1];
    if (skill && last && last.skill === skill) {
      last.items.push({ qr, index });
    } else {
      groups.push({ skill, items: [{ qr, index }] });
    }
  });

  return (
    <div className="space-y-5">
      {passageText && (
        <div className="whitespace-pre-wrap rounded-lg border bg-card p-4 text-base leading-relaxed text-foreground">
          {passageText}
        </div>
      )}

      {!hideScoreSummary && (
        <div className="rounded-lg border bg-card p-4">
          <p className="text-2xl font-semibold text-primary">
            {result.scorePercent}%
          </p>
          <p className="text-base text-muted-foreground">
            {result.fullyCorrectCount} de {result.totalQuestions}{" "}
            {result.totalQuestions === 1
              ? t("learning.exerciseResult.correctSingular")
              : t("learning.exerciseResult.correctPlural")}
          </p>
        </div>
      )}

      {feedbackText && (
        <div className="rounded-lg border bg-muted/40 p-4">
          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            {t("learning.exerciseResult.teacherFeedback")}
          </p>
          <p className="mt-1 whitespace-pre-wrap text-base leading-relaxed text-foreground">
            {feedbackText}
          </p>
        </div>
      )}

      <div className={groups.length > 1 ? "space-y-8" : "space-y-3"}>
        {groups.map((group, gi) => (
          <div
            key={
              group.skill
                ? `${group.skill}-${gi}`
                : group.items[0]?.qr.questionId
            }
            className="space-y-4 rounded-lg border p-3 text-base"
          >
            {group.skill && (
              <p className="text-xs font-medium uppercase tracking-wide text-primary">
                {t(`quiz.skills.${group.skill}`)}
              </p>
            )}
            {group.items.map(({ qr, index }) => (
              <QuestionResultBlock
                key={qr.questionId}
                question={byId.get(qr.questionId)}
                result={qr}
                number={index + 1}
                showAllAnswers={showAllAnswers}
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
