import { createFileRoute, Link } from "@tanstack/react-router";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/universidad")({
  head: () => ({
    meta: seo({
      title: "Universidad — Destino: Español",
      description: "Información y recursos para el alumnado universitario.",
      path: "/universidad",
    }),
  }),
  component: UniversityHome,
});

function UniversityHome() {
  const links = [
    ["Horario", "/universidad/horario"],
    ["Exámenes", "/universidad/examenes"],
    ["Noticias", "/universidad/noticias"],
    ["Mi aprendizaje", "/universidad/aprendizaje"],
  ] as const;
  return (
    <div className="mx-auto max-w-5xl px-6 py-14">
      <p className="text-sm font-medium text-primary">Destino: Español · Universidad</p>
      <h1 className="mt-2 font-display text-4xl font-semibold text-primary">Espacio del alumnado universitario</h1>
      <p className="mt-4 max-w-2xl text-muted-foreground">
        Consulta el horario, los próximos exámenes y las novedades del curso.
        El acceso a materiales y tareas se activa por la profesora.
      </p>
      <div className="mt-8 grid gap-4 sm:grid-cols-2">
        {links.map(([label, to]) => (
          <Link key={to} to={to} className="rounded-lg border bg-card p-5 font-medium transition-colors hover:bg-accent">
            {label} →
          </Link>
        ))}
      </div>
    </div>
  );
}
