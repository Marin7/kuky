import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getStudents, studentDisplayName, type Student } from "@/lib/admin";
import { Checkbox } from "@/components/ui/checkbox";

interface Props {
  selected: string[];
  onChange: (ids: string[]) => void;
}

export function StudentMultiSelect({ selected, onChange }: Props) {
  const { t } = useTranslation();
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
    return (
      <p className="text-xs text-muted-foreground">
        {t("admin.students.loading")}
      </p>
    );
  if (students.length === 0)
    return (
      <p className="text-xs text-muted-foreground">
        {t("admin.students.empty")}
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
              <span className="ml-1 text-xs text-muted-foreground">
                {s.email}
              </span>
            )}
          </span>
        </label>
      ))}
    </div>
  );
}
