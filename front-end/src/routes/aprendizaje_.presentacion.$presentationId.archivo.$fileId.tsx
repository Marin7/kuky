import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMe, type UserResponse } from "@/lib/auth";
import {
  getLearning,
  type ActivitySummary,
} from "@/lib/learning";
import { ActivityViewerPrompts } from "@/components/learning/ActivityViewerPrompts";
import { StudentOnlyNotice } from "@/components/StudentOnlyNotice";
import { seo } from "@/lib/seo";

export const Route = createFileRoute(
  "/aprendizaje_/presentacion/$presentationId/archivo/$fileId",
)({
  head: () => ({
    meta: seo({
      title: "Presentación — Destino: Español",
      description: "Consulta la presentación compartida por tu profesora.",
      path: "/aprendizaje/presentacion",
    }),
  }),
  component: PresentationViewerPage,
});

function PresentationViewerPage() {
  const { t } = useTranslation();
  const { presentationId, fileId } = Route.useParams();
  const [user, setUser] = useState<UserResponse | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [activities, setActivities] = useState<ActivitySummary[]>([]);
  const [title, setTitle] = useState<string | undefined>();
  const navigate = useNavigate();

  const loadActivities = useCallback(() => {
    getLearning()
      .then((data) => {
        const pres = data.sharedPresentations.find(
          (p) => p.id === presentationId,
        );
        setTitle(pres?.title);
        setActivities(pres?.activities ?? []);
      })
      .catch(() => {
        setActivities([]);
      });
  }, [presentationId]);

  useEffect(() => {
    getMe()
      .then(setUser)
      .catch(() => {
        setUser(null);
        navigate({ to: "/cuenta" });
      })
      .finally(() => setAuthLoading(false));
  }, []);

  useEffect(() => {
    if (!user) return;
    if (user.role !== "STUDENT" && user.role !== "ADMIN") return;
    loadActivities();
  }, [user, loadActivities]);

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

  return (
    <ActivityViewerPrompts
      presentationId={presentationId}
      fileId={fileId}
      activities={activities}
      title={title}
      onActivitiesChanged={loadActivities}
    />
  );
}
