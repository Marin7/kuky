import { createFileRoute } from "@tanstack/react-router";
import { UniversityScheduleView } from "@/components/university/UniversityScheduleView";

export const Route = createFileRoute("/universidad/horario")({
  component: () => (
    <div className="mx-auto max-w-5xl px-6 py-12">
      <h1 className="font-display text-3xl font-semibold text-primary">Horario</h1>
      <p className="mt-2 text-muted-foreground">Clases y cambios puntuales del curso.</p>
      <div className="mt-8"><UniversityScheduleView /></div>
    </div>
  ),
});
