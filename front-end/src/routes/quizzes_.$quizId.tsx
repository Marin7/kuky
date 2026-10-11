import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMe } from "@/lib/auth";
import { QuizTakeView } from "@/components/learning/QuizTakeView";
import { seo } from "@/lib/seo";

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

function QuizTakePage() {
  const { t } = useTranslation();
  const { quizId } = Route.useParams();
  const [auth, setAuth] = useState<"loading" | "ok" | "error">("loading");

  useEffect(() => {
    getMe()
      .then(() => setAuth("ok"))
      .catch(() => setAuth("error"));
  }, []);

  return (
    <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 sm:py-8">
      <Link
        to="/aprendizaje"
        className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        {t("quiz.back")}
      </Link>
      {auth === "error" ? (
        <p className="text-sm text-destructive">{t("quiz.loginRequired")}</p>
      ) : auth === "loading" ? (
        <p className="text-sm text-muted-foreground">{t("quiz.loading")}</p>
      ) : (
        <QuizTakeView quizId={quizId} />
      )}
    </div>
  );
}
