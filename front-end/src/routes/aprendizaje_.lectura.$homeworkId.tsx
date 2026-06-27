import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMe, type UserResponse } from "@/lib/auth";
import { HomeworkReadingPage } from "@/components/learning/HomeworkReadingPage";
import type { HomeworkFormat } from "@/lib/learning";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/aprendizaje_/lectura/$homeworkId")({
  validateSearch: (
    search: Record<string, unknown>,
  ): { format: HomeworkFormat } => ({
    format: search.format === "EXERCISE" ? "EXERCISE" : "MANUAL",
  }),
  head: () => ({
    meta: seo({
      title: "Lectura — Español con Paula",
      description: "Lee el texto y resuelve tu tarea de lectura.",
      path: "/aprendizaje/lectura",
    }),
  }),
  component: LecturaPage,
});

function LecturaPage() {
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

  return <HomeworkReadingPage homeworkId={homeworkId} format={format} />;
}
