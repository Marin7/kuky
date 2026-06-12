import { createFileRoute } from "@tanstack/react-router";
import { HomeworkEditorPage } from "@/components/admin/homework/HomeworkEditorPage";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/panel_/tareas/nueva")({
  head: () => ({
    meta: seo({
      title: "Nueva tarea — Español con Paula",
      description: "Crea una tarea para tus alumnos.",
      path: "/panel/tareas/nueva",
    }),
  }),
  component: NuevaTareaPage,
});

function NuevaTareaPage() {
  return <HomeworkEditorPage />;
}
