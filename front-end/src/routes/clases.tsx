import { createFileRoute } from "@tanstack/react-router";
import { Placeholder } from "@/components/Placeholder";

export const Route = createFileRoute("/clases")({
  head: () => ({
    meta: [
      { title: "Clases y materiales — Español con Paula" },
      { name: "description", content: "Explora las clases disponibles y accede a materiales de estudio." },
    ],
  }),
  component: () => (
    <Placeholder
      eyebrow="Clases"
      title="Clases y materiales"
      description="Aquí encontrarás el catálogo de clases, niveles disponibles y los materiales de estudio para descargar."
    />
  ),
});
