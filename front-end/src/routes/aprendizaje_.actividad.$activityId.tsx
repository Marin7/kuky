import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMe, type UserResponse } from "@/lib/auth";
import { ActivityPanel } from "@/components/learning/ActivityPanel";
import { StudentOnlyNotice } from "@/components/StudentOnlyNotice";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/aprendizaje_/actividad/$activityId")({
  head: () => ({
    meta: seo({
      title: "Actividad — Destino: Español",
      description: "Completa la actividad de tu presentación.",
      path: "/aprendizaje/actividad",
    }),
  }),
  component: ActividadPage,
});

function ActividadPage() {
  const { t } = useTranslation();
  const { activityId } = Route.useParams();
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
      <div className="mx-auto max-w-3xl px-6 py-16 text-center">
        <p className="animate-pulse text-sm text-muted-foreground">
          {t("common.loading")}
        </p>
      </div>
    );
  }

  if (!user) return null;

  if (user.role !== "STUDENT" && user.role !== "ADMIN") {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16">
        <StudentOnlyNotice />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 sm:py-8">
      <Link
        to="/aprendizaje"
        className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        {t("learning.activities.back")}
      </Link>
      <ActivityPanel activityId={activityId} />
    </div>
  );
}
