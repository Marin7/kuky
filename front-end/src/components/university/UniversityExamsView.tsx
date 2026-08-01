import { useEffect, useState } from "react";
import { getUniversityExams, type UniversityExam } from "@/lib/university";

export function UniversityExamsView() {
  const [exams, setExams] = useState<UniversityExam[] | null>(null);

  useEffect(() => {
    getUniversityExams().then(setExams).catch(() => setExams([]));
  }, []);

  if (!exams) return <p className="animate-pulse text-muted-foreground">Cargando exámenes…</p>;
  if (!exams.length) return <p className="text-muted-foreground">No hay exámenes publicados por ahora.</p>;

  return (
    <div className="space-y-3">
      {exams.map((exam) => (
        <article key={exam.id} className="rounded-lg border bg-card p-5">
          <p className="text-sm font-medium text-primary">
            {new Intl.DateTimeFormat("es", {
              dateStyle: "full",
              timeStyle: "short",
            }).format(new Date(exam.examAt))}
          </p>
          <h2 className="mt-1 text-xl font-semibold">{exam.title}</h2>
          {exam.description && <p className="mt-2 whitespace-pre-wrap text-muted-foreground">{exam.description}</p>}
        </article>
      ))}
    </div>
  );
}
