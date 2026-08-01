import { createFileRoute } from "@tanstack/react-router";
import { UniversityNewsView } from "@/components/university/UniversityNewsView";

export const Route = createFileRoute("/universidad/noticias")({
  component: () => (
    <div className="mx-auto max-w-4xl px-6 py-12">
      <h1 className="font-display text-3xl font-semibold text-primary">Noticias</h1>
      <p className="mt-2 text-muted-foreground">Avisos y novedades del curso.</p>
      <div className="mt-8"><UniversityNewsView /></div>
    </div>
  ),
});
