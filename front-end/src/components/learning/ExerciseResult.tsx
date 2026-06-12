import { useTranslation } from "react-i18next";
import type { ExerciseResult, StudentQuestion } from "@/lib/learning";

interface Props {
  questions: StudentQuestion[];
  result: ExerciseResult;
}

function optionLabels(question: StudentQuestion, ids: string[]): string {
  return question.options
    .filter((o) => ids.includes(o.id))
    .map((o) => o.label)
    .join(", ");
}

export function ExerciseResult({ questions, result }: Props) {
  const { t } = useTranslation();
  const byId = new Map(questions.map((q) => [q.id, q]));

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

          return (
            <div key={qr.questionId} className="rounded-lg border p-3 text-sm">
              <div className="flex items-start justify-between gap-3">
                <p className="font-medium text-foreground">
                  {i + 1}. {question?.prompt}
                </p>
                <span
                  className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${badge.cls}`}
                >
                  {badge.text}
                </span>
              </div>
              {!qr.correct && correctText && (
                <p className="mt-2 text-muted-foreground">
                  <span className="font-medium text-foreground">
                    {t("learning.exerciseResult.correctAnswer")}{" "}
                  </span>
                  {correctText}
                </p>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
