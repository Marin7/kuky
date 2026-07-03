import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getStudents,
  revokeStudent,
  grantExtendedClass,
  revokeExtendedClass,
  getExtendedClassEligibleStudentIds,
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
  const [extendedEligibleIds, setExtendedEligibleIds] = useState<Set<string>>(
    new Set(),
  );
  const [togglingExtended, setTogglingExtended] = useState<string | null>(null);
  const [extendedError, setExtendedError] = useState<string | null>(null);

  useEffect(() => {
    getStudents()
      .then(setStudents)
      .catch(() => setStudents([]))
      .finally(() => setLoading(false));
    getExtendedClassEligibleStudentIds()
      .then((ids) => setExtendedEligibleIds(new Set(ids)))
      .catch(() => setExtendedEligibleIds(new Set()));
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

  const handleToggleExtendedClass = async (s: Student) => {
    const isEligible = extendedEligibleIds.has(s.id);
    const confirmKey = isEligible
      ? "admin.students.extendedClass.revokeConfirm"
      : "admin.students.extendedClass.grantConfirm";
    if (!window.confirm(t(confirmKey))) return;
    setTogglingExtended(s.id);
    setExtendedError(null);
    try {
      const updated = isEligible
        ? await revokeExtendedClass(s.id)
        : await grantExtendedClass(s.id);
      setExtendedEligibleIds((prev) => {
        const next = new Set(prev);
        if (updated.extendedClassEligible) next.add(s.id);
        else next.delete(s.id);
        return next;
      });
    } catch {
      setExtendedError(t("admin.students.extendedClass.error"));
    } finally {
      setTogglingExtended(null);
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
      {extendedError && (
        <p className="text-sm text-destructive mb-2">{extendedError}</p>
      )}
      <ul className="divide-y rounded-lg border">
        {students.map((s) => {
          const name = studentDisplayName(s);
          const hasRealName = name !== s.email.split("@")[0];
          const isExtendedEligible = extendedEligibleIds.has(s.id);
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
                {isExtendedEligible && (
                  <span className="text-xs rounded-full bg-primary/10 text-primary px-2 py-0.5">
                    {t("admin.students.extendedClass.eligibleBadge")}
                  </span>
                )}
                <Button
                  variant="outline"
                  size="sm"
                  disabled={togglingExtended === s.id}
                  onClick={() => handleToggleExtendedClass(s)}
                  className="h-7 text-xs"
                >
                  {togglingExtended === s.id
                    ? t("admin.students.extendedClass.saving")
                    : isExtendedEligible
                      ? t("admin.students.extendedClass.revoke")
                      : t("admin.students.extendedClass.grant")}
                </Button>
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
