import { useTranslation } from "react-i18next";
import type { ExerciseResult, StudentQuestion } from "@/lib/learning";

interface Props {
  questions: StudentQuestion[];
  result: ExerciseResult;
  /** When true (teacher view), always show the student's answer, not only on mistakes. */
  showAllAnswers?: boolean;
}

function optionLabels(question: StudentQuestion, ids: string[]): string {
  return question.options
    .filter((o) => ids.includes(o.id))
    .map((o) => o.label)
    .join(", ");
}

function displayOrDash(value: string | null | undefined, empty: string): string {
  const trimmed = value?.trim();
  return trimmed ? trimmed : empty;
}

export function ExerciseResult({ questions, result, showAllAnswers = false }: Props) {
  const { t } = useTranslation();
  const byId = new Map(questions.map((q) => [q.id, q]));
  const noAnswer = t("learning.exerciseResult.noAnswer");

  return (
    <div className="space-y-5">
      <div className="rounded-lg border bg-card p-4">
        <p className="text-2xl font-semibold text-primary">
          {result.scorePercent}%
        </p>
        <p className="text-sm text-muted-foreground">
          {result.fullyCorrectCount} de {result.totalQuestions}{" "}
          {result.totalQuestions === 1
            ? t("learning.exerciseResult.correctSingular")
            : t("learning.exerciseResult.correctPlural")}
        </p>
      </div>

      <div className="space-y-3">
        {result.questions.map((qr, i) => {
          const question = byId.get(qr.questionId);
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
                ? optionLabels(question, qr.correctOptionIds)
                : "";
          const unitResults = qr.unitResults ?? [];
          const studentChoiceText =
            question && (qr.selectedOptionIds?.length ?? 0) > 0
              ? optionLabels(question, qr.selectedOptionIds ?? [])
              : "";
          const studentFillText = qr.answerText;
          const hasLegacyStudentAnswer =
            studentChoiceText.length > 0 ||
            (studentFillText != null && studentFillText !== "");
          const showLegacyDetail = showAllAnswers || !qr.correct || partial;

          return (
            <div key={qr.questionId} className="rounded-lg border p-3 text-sm">
              <div className="flex items-start justify-between gap-3">
                <p className="whitespace-pre-wrap font-medium text-foreground">
                  {i + 1}. {question?.prompt}
                </p>
                <span
                  className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${badge.cls}`}
                >
                  {badge.text}
                </span>
              </div>
              {unitResults.length > 0 ? (
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
                          {!u.correct &&
                            u.expectedDisplay &&
                            u.expectedDisplay.length > 0 && (
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
                showLegacyDetail && (
                  <div className="mt-2 space-y-1 text-muted-foreground">
                    <p>
                      <span className="font-medium text-foreground">
                        {t("learning.exerciseResult.yourAnswer")}{" "}
                      </span>
                      {hasLegacyStudentAnswer
                        ? studentChoiceText ||
                          displayOrDash(studentFillText, noAnswer)
                        : noAnswer}
                    </p>
                    {(!qr.correct || partial) && correctText && (
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
        })}
      </div>
    </div>
  );
}
