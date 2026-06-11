import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { getMe, type UserResponse } from "@/lib/auth";
import { LearningView } from "@/components/learning/LearningView";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/aprendizaje")({
  head: () => ({
    meta: seo({
      title: "Mi aprendizaje — Español con Paula",
      description:
        "Tu espacio de aprendizaje: presentación de las clases, tus clases anteriores y tus tareas.",
      path: "/aprendizaje",
    }),
  }),
  component: AprendizajePage,
});

function AprendizajePage() {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    getMe()
      .then(setUser)
      .catch(() => {
        setUser(null);
        // Section is logged-in only — send guests to sign in.
        navigate({ to: "/cuenta" });
      })
      .finally(() => setAuthLoading(false));
  }, []);

  if (authLoading) {
    return (
      <div className="mx-auto max-w-5xl px-6 py-16 text-center">
        <p className="text-muted-foreground text-sm animate-pulse">Cargando…</p>
      </div>
    );
  }

  if (!user) return null;

  return <LearningView />;
}
