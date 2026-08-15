import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMe, type UserResponse } from "@/lib/auth";
import { listMyQuizzes, type QuizListItem } from "@/lib/quiz";
import { StudentOnlyNotice } from "@/components/StudentOnlyNotice";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/quizzes")({
  head: () => ({
    meta: seo({
      title: "Quizzes — Destino: Español",
      description: "Tus quizzes asignados.",
      path: "/quizzes",
    }),
  }),
  component: QuizzesPage,
});

function QuizzesPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [user, setUser] = useState<UserResponse | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [items, setItems] = useState<QuizListItem[]>([]);
  const [loadError, setLoadError] = useState(false);

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
    if (!user || (user.role !== "STUDENT" && user.role !== "ADMIN")) return;
    listMyQuizzes()
      .then(setItems)
      .catch(() => setLoadError(true));
  }, [user]);

  if (authLoading) {
    return (
      <p className="mx-auto max-w-3xl px-6 py-16 text-center text-sm text-muted-foreground">
        {t("common.loading")}
      </p>
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
    <div className="mx-auto max-w-3xl px-6 py-12">
      <h1 className="font-display text-3xl font-semibold text-primary">
        {t("quiz.title")}
      </h1>
      {loadError ? (
        <p className="mt-6 text-sm text-destructive">{t("quiz.loadError")}</p>
      ) : items.length === 0 ? (
        <p className="mt-6 text-sm text-muted-foreground">{t("quiz.empty")}</p>
      ) : (
        <ul className="mt-6 space-y-3">
          {items.map((q) => (
            <li key={q.id}>
              <Link
                to="/quizzes/$quizId"
                params={{ quizId: q.id }}
                className="block rounded-lg border p-4 hover:bg-accent/30"
              >
                <p className="font-medium">{q.title}</p>
                <p className="text-sm text-muted-foreground">
                  {t(`quiz.status.${q.status}`)}
                </p>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
