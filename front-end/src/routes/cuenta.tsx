import { createFileRoute } from "@tanstack/react-router";
import { Placeholder } from "@/components/Placeholder";

export const Route = createFileRoute("/cuenta")({
  head: () => ({
    meta: [
      { title: "Mi cuenta — Español con Paula" },
      { name: "description", content: "Gestiona tu cuenta y tu perfil de estudiante." },
    ],
  }),
  component: () => (
    <Placeholder
      eyebrow="Mi cuenta"
      title="Gestión de cuenta"
      description="Aquí podrás iniciar sesión, actualizar tu perfil y gestionar tus preferencias."
    />
  ),
});
