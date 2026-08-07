import { createFileRoute } from "@tanstack/react-router";
import { ActivityEditorPage } from "@/components/admin/activities/ActivityEditorPage";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/panel_/actividades/nueva")({
  head: () => ({
    meta: seo({
      title: "Nueva actividad — Destino: Español",
      description: "Crea una actividad vinculada a una presentación.",
      path: "/panel/actividades/nueva",
    }),
  }),
  component: NuevaActividadPage,
});

function NuevaActividadPage() {
  return <ActivityEditorPage />;
}
