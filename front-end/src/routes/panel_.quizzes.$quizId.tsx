import { createFileRoute } from "@tanstack/react-router";
import { QuizEditorPage } from "@/components/quiz/admin/QuizEditorPage";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/panel_/quizzes/$quizId")({
  head: () => ({
    meta: seo({
      title: "Editar prueba de evaluación — Destino: Español",
      description: "Edita una prueba de evaluación de tus alumnos.",
      path: "/panel/quizzes/editar",
    }),
  }),
  component: EditQuizPage,
});

function EditQuizPage() {
  const { quizId } = Route.useParams();
  return <QuizEditorPage quizId={quizId} />;
}
