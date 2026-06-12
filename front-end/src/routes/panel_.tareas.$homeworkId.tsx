import { createFileRoute } from "@tanstack/react-router";
import { HomeworkEditorPage } from "@/components/admin/homework/HomeworkEditorPage";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/panel_/tareas/$homeworkId")({
  head: () => ({
    meta: seo({
      title: "Editar tarea — Español con Paula",
      description: "Edita una tarea de tus alumnos.",
      path: "/panel/tareas/editar",
    }),
  }),
  component: EditarTareaPage,
});

function EditarTareaPage() {
  const { homeworkId } = Route.useParams();
  return <HomeworkEditorPage homeworkId={homeworkId} />;
}
