import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMe } from "@/lib/auth";
import {
  getQuiz,
  submitQuizAnswers,
  type QuizQuestionResult,
  type QuizTakeResponse,
} from "@/lib/quiz";
import { MixedHomeworkForm } from "@/components/learning/MixedHomeworkForm";
import { ExerciseResult } from "@/components/learning/ExerciseResult";
import { seo } from "@/lib/seo";
import type {
  ExerciseResult as ExerciseResultData,
  HeterogeneousAnswerPayload,
  QuestionResult,
} from "@/lib/learning";

export const Route = createFileRoute("/quizzes_/$quizId")({
  head: () => ({
    meta: seo({
      title: "Prueba de evaluación — Destino: Español",
      description: "Haz tu prueba de evaluación.",
      path: "/quizzes",
    }),
  }),
  component: QuizTakePage,
});

function toQuestionResult(r: QuizQuestionResult): QuestionResult {
  return {
    questionId: r.questionId,
    score: r.score,
    correct: r.correct,
    correctOptionIds: r.correctOptionIds,
    acceptedAnswers: r.acceptedAnswers,
    unitResults: r.unitResults,
    selectedOptionIds: r.selectedOptionIds,
  };
}

function autoExerciseResult(quiz: QuizTakeResponse): ExerciseResultData {
  const autoIds = new Set(
    quiz.questions.filter((q) => q.kind !== "FREE_TEXT").map((q) => q.id),
  );
  const questions = (quiz.results ?? [])
    .filter((r) => autoIds.has(r.questionId))
    .map(toQuestionResult);

  let scoreSum = 0;
  let counted = 0;
  let fullyCorrect = 0;
  for (const r of questions) {
    const units =
      r.unitResults && r.unitResults.length > 0
        ? r.unitResults
        : [{ score: r.score, correct: r.correct }];
    for (const u of units) {
      counted++;
      scoreSum += u.score;
      if (u.correct) fullyCorrect++;
    }
  }

  const allAuto = autoIds.size === quiz.questions.length;
  if (allAuto && quiz.scorePercent != null) {
    return {
      scorePercent: quiz.scorePercent,
      fullyCorrectCount: quiz.fullyCorrectCount ?? fullyCorrect,
      totalQuestions: quiz.questionUnitCount ?? counted,
      questions,
    };
  }

  return {
    scorePercent: counted === 0 ? 0 : Math.round((scoreSum / counted) * 100),
    fullyCorrectCount: fullyCorrect,
    totalQuestions: counted,
    questions,
  };
}

function QuizResultSummary({
  quiz,
}: {
  quiz: QuizTakeResponse;
}) {
  const { t } = useTranslation();
  const awaiting = quiz.status === "SUBMITTED";
  const showFinal =
    quiz.status === "GRADED" && quiz.scorePercent != null;
  if (!showFinal && !awaiting && quiz.skills.length === 0) return null;

  return (
    <div className="mt-6 rounded-lg border bg-card p-4">
      {showFinal && (
        <>
          <p className="text-2xl font-semibold text-primary">
            {quiz.scorePercent}%
          </p>
          <p className="text-sm text-muted-foreground">
            {t("learning.mixed.combinedScore")}
          </p>
        </>
      )}
      {awaiting && (
        <div className="rounded-md border border-amber-200 bg-amber-50 p-3 dark:border-amber-900 dark:bg-amber-950/40">
          <p className="text-sm font-medium text-amber-900 dark:text-amber-100">
            {t("learning.mixed.awaitingTeacher")}
          </p>
          {quiz.scorePercent != null && (
            <p className="mt-1 text-sm text-amber-800 dark:text-amber-200">
              {t("learning.mixed.provisionalScore", {
                percent: quiz.scorePercent,
              })}
            </p>
          )}
        </div>
      )}
      {quiz.skills.length > 0 && (
        <>
          <p
            className={`text-sm font-medium ${showFinal || awaiting ? "mt-4" : ""}`}
          >
            {t("quiz.skillsTitle")}
          </p>
          <ul className="mt-2 space-y-1 text-sm text-muted-foreground">
            {quiz.skills.map((s) => (
              <li key={s.skill}>
                {t(`quiz.skills.${s.skill}`)}
                {s.awaitingTeacher
                  ? ` — ${t("quiz.skillAwaiting")}`
                  : s.scorePercent != null
                    ? ` — ${s.scorePercent}%`
                    : ""}
                {s.fullyCorrectCount != null && !s.awaitingTeacher
                  ? ` · ${s.fullyCorrectCount} / ${s.questionUnitCount}`
                  : ""}
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}

function QuizTakePage() {
  const { t } = useTranslation();
  const { quizId } = Route.useParams();
  const [quiz, setQuiz] = useState<QuizTakeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    getQuiz(quizId)
      .then(setQuiz)
      .catch((e: { message?: string }) =>
        setError(e.message ?? t("quiz.loadError")),
      );
  };

  useEffect(() => {
    getMe()
      .then(() => load())
      .catch(() => setError(t("quiz.loginRequired")));
  }, [quizId]);

  if (error) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 sm:py-8">
        <p className="text-sm text-destructive">{error}</p>
        <Link to="/aprendizaje" className="mt-4 inline-block text-sm underline">
          {t("quiz.back")}
        </Link>
      </div>
    );
  }
  if (!quiz) {
    return (
      <p className="mx-auto max-w-3xl px-4 py-6 text-sm text-muted-foreground sm:px-6 sm:py-8">
        {t("quiz.loading")}
      </p>
    );
  }

  const submitted =
    quiz.status === "SUBMITTED" || quiz.status === "GRADED";
  const hasFreeText = quiz.questions.some((q) => q.kind === "FREE_TEXT");
  const autoResult = submitted ? autoExerciseResult(quiz) : null;
  const autoQuestions = quiz.questions.filter((q) => q.kind !== "FREE_TEXT");

  return (
    <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 sm:py-8">
      <Link
        to="/aprendizaje"
        className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        {t("quiz.back")}
      </Link>
      <h1 className="font-display text-2xl font-semibold text-primary sm:text-3xl">
        {quiz.title}
      </h1>
      {quiz.description && (
        <p className="mt-2 whitespace-pre-wrap text-base leading-relaxed text-muted-foreground">
          {quiz.description}
        </p>
      )}

      {submitted && <QuizResultSummary quiz={quiz} />}

      {submitted && !hasFreeText && autoResult ? (
        <div className="mt-6">
          <ExerciseResult
            questions={autoQuestions}
            result={autoResult}
            hideScoreSummary
            showAllAnswers
          />
        </div>
      ) : (
        <>
          <MixedHomeworkForm
            richFreeText
            hideAutoResultsSummary
            hideCombinedScore
            showAllAnswers
            assignment={{
              id: quiz.id,
              status:
                quiz.status === "IN_PROGRESS" || quiz.status === "AVAILABLE"
                  ? "PENDING"
                  : quiz.status,
              questions: quiz.questions,
              answers: quiz.questions
                .filter((q) => q.kind === "FREE_TEXT")
                .map((q) => {
                  const r = quiz.results.find((x) => x.questionId === q.id);
                  return {
                    questionId: q.id,
                    promptSnapshot: q.prompt,
                    text: r?.answerText ?? "",
                    formatted: r?.formatted ?? null,
                    teacherScorePercent:
                      quiz.status === "GRADED"
                        ? (r?.teacherPercent ?? null)
                        : null,
                  };
                }),
              feedbackText: quiz.feedback,
              scorePercent:
                quiz.status === "GRADED" ? quiz.scorePercent : null,
              provisionalScorePercent:
                quiz.status === "SUBMITTED" ? quiz.scorePercent : null,
              result:
                submitted && autoResult && autoResult.questions.length > 0
                  ? autoResult
                  : null,
            }}
            submitAnswers={(_id, answers: HeterogeneousAnswerPayload[]) =>
              submitQuizAnswers(quizId, answers).then((updated) => {
                setQuiz(updated);
                return updated;
              })
            }
          />
        </>
      )}
    </div>
  );
}
