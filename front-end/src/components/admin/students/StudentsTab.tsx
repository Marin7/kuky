import { useEffect, useState } from "react";
import { getStudents, studentDisplayName, type Student } from "@/lib/admin";
import { StudentLink } from "./StudentLink";

export function StudentsTab() {
  const [students, setStudents] = useState<Student[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getStudents()
      .then(setStudents)
      .catch(() => setStudents([]))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <p className="text-sm text-muted-foreground animate-pulse">Cargando…</p>
    );
  }

  if (students.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        Aún no hay alumnos registrados.
      </p>
    );
  }

  return (
    <>
      <p className="text-sm text-muted-foreground mb-4">
        {students.length}{" "}
        {students.length === 1 ? "alumno registrado" : "alumnos registrados"}.
        Haz clic en un nombre para ver su perfil.
      </p>
      <ul className="divide-y rounded-lg border">
        {students.map((s) => {
          const name = studentDisplayName(s);
          const hasRealName = name !== s.email.split("@")[0];
          return (
            <li
              key={s.id}
              className="flex items-center justify-between px-4 py-3"
            >
              <div className="min-w-0">
                <StudentLink student={s} />
                {hasRealName && (
                  <p className="text-xs text-muted-foreground truncate">
                    {s.email}
                  </p>
                )}
              </div>
              {s.username && (
                <span className="text-xs text-muted-foreground ml-2 shrink-0">
                  @{s.username}
                </span>
              )}
            </li>
          );
        })}
      </ul>
    </>
  );
}
