import { useEffect, useState } from "react";
import {
  getUniversitySchedule,
  type UniversitySchedule,
} from "@/lib/university";

const DAYS = [
  "Lunes",
  "Martes",
  "Miércoles",
  "Jueves",
  "Viernes",
  "Sábado",
  "Domingo",
];

export function UniversityScheduleView() {
  const [schedule, setSchedule] = useState<UniversitySchedule | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    getUniversitySchedule().then(setSchedule).catch(() => setError(true));
  }, []);

  if (error) return <p className="text-destructive">No se pudo cargar el horario.</p>;
  if (!schedule) return <p className="animate-pulse text-muted-foreground">Cargando horario…</p>;

  return (
    <div className="space-y-8">
      <section>
        <h2 className="text-xl font-semibold">Horario semanal</h2>
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          {schedule.templateSessions.length === 0 ? (
            <p className="text-sm text-muted-foreground">Aún no hay clases publicadas.</p>
          ) : (
            schedule.templateSessions.map((session) => (
              <article key={session.id} className="rounded-lg border bg-card p-4">
                <p className="font-medium">{DAYS[session.dayOfWeek - 1]}</p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {session.startTime.slice(0, 5)}–{session.endTime.slice(0, 5)}
                  {schedule.viewerMode === "FULL_LABELED" && ` · ${session.level === "BEGINNER" ? "Inicial" : "Intermedio"}`}
                </p>
                {session.title && <p className="mt-2 text-sm">{session.title}</p>}
              </article>
            ))
          )}
        </div>
      </section>
      {schedule.exceptions.length > 0 && (
        <section>
          <h2 className="text-xl font-semibold">Cambios puntuales</h2>
          <ul className="mt-3 space-y-2 text-sm">
            {schedule.exceptions.map((exception) => (
              <li key={exception.id} className="rounded-md border p-3">
                <strong>{exception.exceptionDate}</strong>:{" "}
                {exception.kind === "CANCEL"
                  ? "clase cancelada"
                  : `${exception.startTime?.slice(0, 5)}–${exception.endTime?.slice(0, 5)}${exception.title ? ` · ${exception.title}` : ""}`}
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}
