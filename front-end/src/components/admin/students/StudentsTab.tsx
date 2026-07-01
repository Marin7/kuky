import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getStudents,
  revokeStudent,
  studentDisplayName,
  type Student,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { StudentLink } from "./StudentLink";

export function StudentsTab() {
  const { t } = useTranslation();
  const [students, setStudents] = useState<Student[]>([]);
  const [loading, setLoading] = useState(true);
  const [revoking, setRevoking] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getStudents()
      .then(setStudents)
      .catch(() => setStudents([]))
      .finally(() => setLoading(false));
  }, []);

  const handleRevoke = async (id: string) => {
    if (!window.confirm(t("admin.students.revokeConfirm"))) return;
    setRevoking(id);
    setError(null);
    try {
      await revokeStudent(id);
      setStudents((prev) => prev.filter((s) => s.id !== id));
    } catch {
      setError(t("admin.students.revokeError"));
    } finally {
      setRevoking(null);
    }
  };

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
      {error && <p className="text-sm text-destructive mb-2">{error}</p>}
      <ul className="divide-y rounded-lg border">
        {students.map((s) => {
          const name = studentDisplayName(s);
          const hasRealName = name !== s.email.split("@")[0];
          return (
            <li
              key={s.id}
              className="flex items-center justify-between gap-3 px-4 py-3"
            >
              <div className="min-w-0">
                <StudentLink student={s} />
                {hasRealName && (
                  <p className="text-xs text-muted-foreground truncate">
                    {s.email}
                  </p>
                )}
              </div>
              <div className="flex items-center gap-2 shrink-0">
                {s.username && (
                  <span className="text-xs text-muted-foreground">
                    @{s.username}
                  </span>
                )}
                <Button
                  variant="outline"
                  size="sm"
                  disabled={revoking === s.id}
                  onClick={() => handleRevoke(s.id)}
                  className="h-7 text-xs"
                >
                  {revoking === s.id
                    ? t("admin.students.revoking")
                    : t("admin.students.revoke")}
                </Button>
              </div>
            </li>
          );
        })}
      </ul>
    </>
  );
}
