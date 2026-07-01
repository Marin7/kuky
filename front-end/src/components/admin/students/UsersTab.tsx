import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getRegisteredUsers,
  promoteToStudent,
  studentDisplayName,
  type RegisteredUser,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";

export function UsersTab() {
  const { t } = useTranslation();
  const [users, setUsers] = useState<RegisteredUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [promoting, setPromoting] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    getRegisteredUsers()
      .then(setUsers)
      .catch(() => setUsers([]))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const handlePromote = async (id: string) => {
    setPromoting(id);
    setError(null);
    try {
      await promoteToStudent(id);
      setUsers((prev) => prev.filter((u) => u.id !== id));
    } catch {
      setError(t("admin.users.promoteError"));
    } finally {
      setPromoting(null);
    }
  };

  if (loading) {
    return (
      <p className="text-sm text-muted-foreground animate-pulse">
        {t("admin.users.loading")}
      </p>
    );
  }

  if (users.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">{t("admin.users.empty")}</p>
    );
  }

  return (
    <>
      <p className="text-sm text-muted-foreground mb-4">
        {users.length === 1
          ? t("admin.users.countSingular", { count: users.length })
          : t("admin.users.countPlural", { count: users.length })}{" "}
        {t("admin.users.hint")}
      </p>
      {error && <p className="text-sm text-destructive mb-2">{error}</p>}
      <ul className="divide-y rounded-lg border">
        {users.map((u) => {
          const name = studentDisplayName(u);
          const hasRealName = name !== u.email.split("@")[0];
          return (
            <li
              key={u.id}
              className="flex items-center justify-between gap-3 px-4 py-3"
            >
              <div className="min-w-0">
                <p className="font-medium truncate">{name}</p>
                {hasRealName && (
                  <p className="text-xs text-muted-foreground truncate">
                    {u.email}
                  </p>
                )}
              </div>
              <Button
                variant="outline"
                size="sm"
                disabled={promoting === u.id}
                onClick={() => handlePromote(u.id)}
                className="h-7 text-xs shrink-0"
              >
                {promoting === u.id
                  ? t("admin.users.promoting")
                  : t("admin.users.makeStudent")}
              </Button>
            </li>
          );
        })}
      </ul>
    </>
  );
}
