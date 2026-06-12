import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMe, type UserResponse } from "@/lib/auth";
import { HomeworkExercisePage } from "@/components/learning/HomeworkExercisePage";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/aprendizaje_/tarea/$homeworkId")({
  head: () => ({
    meta: seo({
      title: "Ejercicio — Español con Paula",
      description: "Resuelve tu ejercicio autocorregible.",
      path: "/aprendizaje/tarea",
    }),
  }),
  component: TareaEjercicioPage,
});

function TareaEjercicioPage() {
  const { t } = useTranslation();
  const { homeworkId } = Route.useParams();
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
      <div className="mx-auto max-w-2xl px-6 py-16 text-center">
        <p className="animate-pulse text-sm text-muted-foreground">
          {t("common.loading")}
        </p>
      </div>
    );
  }

  if (!user) return null;

  return <HomeworkExercisePage homeworkId={homeworkId} />;
}
