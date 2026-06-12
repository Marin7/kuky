import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getStudents, studentDisplayName, type Student } from "@/lib/admin";
import { StudentLink } from "./StudentLink";

export function StudentsTab() {
  const { t } = useTranslation();
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
      <p className="text-sm text-muted-foreground animate-pulse">
        {t("admin.students.loading")}
      </p>
    );
  }

  if (students.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("admin.students.empty")}
      </p>
    );
  }

  return (
    <>
      <p className="text-sm text-muted-foreground mb-4">
        {students.length === 1
          ? t("admin.students.countSingular", { count: students.length })
          : t("admin.students.countPlural", { count: students.length })}{" "}
        {t("admin.students.hint")}
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
