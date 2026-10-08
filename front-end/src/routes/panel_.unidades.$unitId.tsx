import { createFileRoute } from "@tanstack/react-router";
import { UnitEditorPage } from "@/components/admin/units/UnitEditorPage";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/panel_/unidades/$unitId")({
  head: () => ({
    meta: seo({
      title: "Editar unidad — Destino: Español",
      description: "PDF de la unidad, actividades por página y tareas.",
      path: "/panel/unidades/editar",
    }),
  }),
  component: EditarUnidadPage,
});

function EditarUnidadPage() {
  const { unitId } = Route.useParams();
  return <UnitEditorPage unitId={unitId} />;
}
