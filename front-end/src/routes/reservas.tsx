import { createFileRoute } from "@tanstack/react-router";
import { Placeholder } from "@/components/Placeholder";

export const Route = createFileRoute("/reservas")({
  head: () => ({
    meta: [
      { title: "Reservas — Español con Paula" },
      { name: "description", content: "Reserva tu clase de español y consulta tus citas." },
    ],
  }),
  component: () => (
    <Placeholder
      eyebrow="Reservas"
      title="Resumen de reservas"
      description="Desde aquí podrás reservar nuevas clases y consultar el calendario de tus citas."
    />
  ),
});
