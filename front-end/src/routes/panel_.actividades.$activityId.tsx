import { createFileRoute } from "@tanstack/react-router";
import { ActivityEditorPage } from "@/components/admin/activities/ActivityEditorPage";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/panel_/actividades/$activityId")({
  head: () => ({
    meta: seo({
      title: "Editar actividad — Destino: Español",
      description: "Edita una actividad de presentación.",
      path: "/panel/actividades/editar",
    }),
  }),
  component: EditarActividadPage,
});

function EditarActividadPage() {
  const { activityId } = Route.useParams();
  return <ActivityEditorPage activityId={activityId} />;
}
