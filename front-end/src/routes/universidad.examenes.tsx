import { createFileRoute } from "@tanstack/react-router";
import { UniversityExamsView } from "@/components/university/UniversityExamsView";

export const Route = createFileRoute("/universidad/examenes")({
  component: () => (
    <div className="mx-auto max-w-4xl px-6 py-12">
      <h1 className="font-display text-3xl font-semibold text-primary">Exámenes</h1>
      <p className="mt-2 text-muted-foreground">Fechas publicadas para el curso.</p>
      <div className="mt-8"><UniversityExamsView /></div>
    </div>
  ),
});
