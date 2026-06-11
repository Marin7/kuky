import { useEffect, useState } from "react";
import { getStudents, studentDisplayName, type Student } from "@/lib/admin";
import { Checkbox } from "@/components/ui/checkbox";

interface Props {
  selected: string[];
  onChange: (ids: string[]) => void;
}

/** Multi-select student picker (reused for homework assignment and presentation sharing). */
export function StudentMultiSelect({ selected, onChange }: Props) {
  const [students, setStudents] = useState<Student[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getStudents()
      .then(setStudents)
      .catch(() => setStudents([]))
      .finally(() => setLoading(false));
  }, []);

  const toggle = (id: string) => {
    onChange(
      selected.includes(id)
        ? selected.filter((s) => s !== id)
        : [...selected, id],
    );
  };

  if (loading)
    return <p className="text-xs text-muted-foreground">Cargando alumnos…</p>;
  if (students.length === 0)
    return (
      <p className="text-xs text-muted-foreground">
        No hay alumnos registrados todavía.
      </p>
    );

  return (
    <div className="max-h-40 space-y-2 overflow-y-auto rounded-md border p-3">
      {students.map((s) => (
        <label key={s.id} className="flex items-center gap-2 text-sm">
          <Checkbox
            checked={selected.includes(s.id)}
            onCheckedChange={() => toggle(s.id)}
          />
          <span>
            {studentDisplayName(s)}
            {s.firstName && (
              <span className="ml-1 text-xs text-muted-foreground">{s.email}</span>
            )}
          </span>
        </label>
      ))}
    </div>
  );
}
