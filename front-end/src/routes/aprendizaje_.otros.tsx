import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMe, type UserResponse } from "@/lib/auth";
import { UnitLearningView } from "@/components/learning/UnitLearningView";
import { StudentOnlyNotice } from "@/components/StudentOnlyNotice";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/aprendizaje_/otros")({
  head: () => ({
    meta: seo({
      title: "Otros materiales — Destino: Español",
      description: "Presentaciones y tareas fuera de una unidad.",
      path: "/aprendizaje/otros",
    }),
  }),
  component: OtrosPage,
});

function OtrosPage() {
  const { t } = useTranslation();
  const [user, setUser] = useState<UserResponse | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    getMe()
      .then(setUser)
      .catch(() => {
        setUser(null);
        navigate({ to: "/cuenta" });
      })
      .finally(() => setAuthLoading(false));
  }, []);

  if (authLoading) {
    return (
      <div className="mx-auto max-w-5xl px-6 py-16 text-center">
        <p className="animate-pulse text-sm text-muted-foreground">
          {t("common.loading")}
        </p>
      </div>
    );
  }

  if (!user) return null;

  if (user.role !== "STUDENT" && user.role !== "ADMIN") {
    return (
      <div className="mx-auto max-w-5xl px-6 py-16">
        <StudentOnlyNotice />
      </div>
    );
  }

  return <UnitLearningView unitId={null} />;
}
