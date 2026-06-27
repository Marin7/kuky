import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMe, type UserResponse } from "@/lib/auth";
import { HomeworkListeningPage } from "@/components/learning/HomeworkListeningPage";
import type { HomeworkFormat } from "@/lib/learning";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/aprendizaje_/escucha/$homeworkId")({
  validateSearch: (
    search: Record<string, unknown>,
  ): { format: HomeworkFormat } => ({
    format: search.format === "EXERCISE" ? "EXERCISE" : "MANUAL",
  }),
  head: () => ({
    meta: seo({
      title: "Escucha — Español con Paula",
      description: "Escucha el audio y resuelve tu tarea de comprensión.",
      path: "/aprendizaje/escucha",
    }),
  }),
  component: EscuchaPage,
});

function EscuchaPage() {
  const { t } = useTranslation();
  const { homeworkId } = Route.useParams();
  const { format } = Route.useSearch();
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

  return <HomeworkListeningPage homeworkId={homeworkId} format={format} />;
}
