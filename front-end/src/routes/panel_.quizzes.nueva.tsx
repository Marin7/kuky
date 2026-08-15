import { createFileRoute } from "@tanstack/react-router";
import { QuizEditorPage } from "@/components/quiz/admin/QuizEditorPage";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/panel_/quizzes/nueva")({
  head: () => ({
    meta: seo({
      title: "Nueva prueba de evaluación — Destino: Español",
      description: "Crea una prueba de evaluación para tus alumnos.",
      path: "/panel/quizzes/nueva",
    }),
  }),
  component: () => <QuizEditorPage />,
});
