import { Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import {
  downloadUniversityPresentation,
  getUniversityLearning,
  submitUniversityHomework,
  type UniversityLearning,
} from "@/lib/university";
import { UniversityOnlyNotice } from "./UniversityOnlyNotice";

export function UniversityLearningView() {
  const [data, setData] = useState<UniversityLearning | null>(null);
  const [answer, setAnswer] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getUniversityLearning().then(setData).catch(() => setError("No se pudo cargar tu aprendizaje."));
  }, []);

  const submitManual = async (id: string) => {
    setSaving(id);
    try {
      const updated = await submitUniversityHomework(id, [{ text: answer[id] ?? "" }]);
      setData((current) =>
        current
          ? { ...current, homework: current.homework.map((item) => item.id === id ? { ...item, ...updated } : item) }
          : current,
      );
    } catch {
      setError("No se pudo enviar la tarea.");
    } finally {
      setSaving(null);
    }
  };

  if (error && !data) {
    return (
      <div className="space-y-5">
        <UniversityOnlyNotice />
        <p className="text-destructive">{error}</p>
      </div>
    );
  }
  if (!data) return <p className="animate-pulse text-muted-foreground">Cargando materiales…</p>;

  return (
    <div className="space-y-10">
      <div>
        <p className="text-sm text-muted-foreground">
          Nivel: {data.level === "BEGINNER" ? "Inicial" : "Intermedio"}
        </p>
        <h2 className="mt-1 text-2xl font-semibold">Materiales</h2>
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          {data.presentations.length ? data.presentations.map((item) => (
            <article key={item.id} className="rounded-lg border p-4">
              <p className="font-medium">{item.title}</p>
              {item.hasFile ? (
                <button
                  className="mt-3 text-sm text-primary underline"
                  onClick={() => downloadUniversityPresentation(item.id, `${item.title}.pptx`)}
                >
                  Descargar
                </button>
              ) : <p className="mt-2 text-sm text-muted-foreground">Archivo pendiente.</p>}
            </article>
          )) : <p className="text-sm text-muted-foreground">No hay materiales disponibles.</p>}
        </div>
      </div>

      <section>
        <h2 className="text-2xl font-semibold">Tareas</h2>
        <div className="mt-4 space-y-4">
          {data.homework.length ? data.homework.map((item) => (
            <article key={item.id} className="rounded-lg border p-5">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <h3 className="font-medium">{item.title}</h3>
                  {item.dueOn && <p className="mt-1 text-sm text-muted-foreground">Fecha límite: {item.dueOn}</p>}
                </div>
                <span className="rounded bg-secondary px-2 py-1 text-xs">{item.status}</span>
              </div>
              {item.format === "EXERCISE" ? (
                <Link to="/universidad/aprendizaje/tarea/$homeworkId" params={{ homeworkId: item.id }} className="mt-4 inline-block text-sm text-primary underline">
                  {item.status === "GRADED" ? "Ver resultado" : "Empezar ejercicio"}
                </Link>
              ) : (
                <div className="mt-4">
                  <textarea
                    value={answer[item.id] ?? ""}
                    onChange={(event) => setAnswer((current) => ({ ...current, [item.id]: event.target.value }))}
                    rows={4}
                    className="w-full rounded-md border bg-background p-3 text-sm"
                    placeholder="Escribe tu respuesta aquí…"
                  />
                  <button
                    disabled={saving === item.id}
                    onClick={() => submitManual(item.id)}
                    className="mt-2 rounded-md bg-primary px-3 py-2 text-sm font-medium text-primary-foreground disabled:opacity-50"
                  >
                    {saving === item.id ? "Enviando…" : "Entregar tarea"}
                  </button>
                </div>
              )}
            </article>
          )) : <p className="text-sm text-muted-foreground">No hay tareas disponibles.</p>}
        </div>
      </section>
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}
