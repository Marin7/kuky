import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMe } from "@/lib/auth";
import {
  getQuiz,
  submitQuizAnswers,
  type QuizTakeResponse,
} from "@/lib/quiz";
import { MixedHomeworkForm } from "@/components/learning/MixedHomeworkForm";
import { seo } from "@/lib/seo";
import type { HeterogeneousAnswerPayload } from "@/lib/learning";

export const Route = createFileRoute("/quizzes/$quizId")({
  head: () => ({
    meta: seo({
      title: "Quiz — Destino: Español",
      description: "Haz tu quiz.",
      path: "/quizzes",
    }),
  }),
  component: QuizTakePage,
});

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
      <div className="mx-auto max-w-3xl px-6 py-12">
        <p className="text-sm text-destructive">{error}</p>
        <Link to="/quizzes" className="mt-4 inline-block text-sm underline">
          {t("quiz.back")}
        </Link>
      </div>
    );
  }
  if (!quiz) {
    return (
      <p className="mx-auto max-w-3xl px-6 py-12 text-sm text-muted-foreground">
        {t("quiz.loading")}
      </p>
    );
  }

  const formStatus =
    quiz.status === "IN_PROGRESS" || quiz.status === "AVAILABLE"
      ? "PENDING"
      : quiz.status;

  return (
    <div className="mx-auto max-w-3xl px-6 py-12">
      <Link to="/quizzes" className="text-sm text-muted-foreground hover:text-foreground">
        {t("quiz.back")}
      </Link>
      <h1 className="mt-4 font-display text-3xl font-semibold text-primary">
        {quiz.title}
      </h1>
      {quiz.description && (
        <p className="mt-2 text-muted-foreground">{quiz.description}</p>
      )}
      {(quiz.status === "SUBMITTED" || quiz.status === "GRADED") && (
        <div className="mt-4 space-y-2 rounded-lg border p-4 text-sm">
          {quiz.status === "SUBMITTED" && <p>{t("quiz.awaiting")}</p>}
          {quiz.scorePercent != null && quiz.fullyCorrectCount != null && (
            <p>
              {t("quiz.score", {
                percent: quiz.scorePercent,
                correct: quiz.fullyCorrectCount,
                total: quiz.questionUnitCount ?? 0,
              })}
            </p>
          )}
          {quiz.skills.length > 0 && (
            <div>
              <p className="font-medium">{t("quiz.skillsTitle")}</p>
              <ul className="mt-1 grid gap-1 sm:grid-cols-2">
                {quiz.skills.map((s) => (
                  <li key={s.skill}>
                    {t(`quiz.skills.${s.skill}`)}
                    {s.awaitingTeacher
                      ? ` — ${t("quiz.skillAwaiting")}`
                      : ` — ${s.scorePercent}%`}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
      <MixedHomeworkForm
        assignment={{
          id: quiz.id,
          status: formStatus,
          questions: quiz.questions,
          result:
            quiz.status === "IN_PROGRESS" || quiz.status === "AVAILABLE"
              ? null
              : {
                  scorePercent: quiz.scorePercent ?? 0,
                  fullyCorrectCount: quiz.fullyCorrectCount ?? 0,
                  totalQuestions: quiz.questionUnitCount ?? 0,
                  questions: (quiz.results ?? []).map((r) => ({
                    questionId: r.questionId,
                    score: r.score,
                    correct: r.correct,
                    correctOptionIds: r.correctOptionIds,
                    acceptedAnswers: r.acceptedAnswers,
                    unitResults: r.unitResults,
                    selectedOptionIds: r.selectedOptionIds,
                  })),
                },
        }}
        submitAnswers={(_id, answers: HeterogeneousAnswerPayload[]) =>
          submitQuizAnswers(quizId, answers).then((updated) => {
            setQuiz(updated);
            return updated;
          })
        }
      />
    </div>
  );
}
