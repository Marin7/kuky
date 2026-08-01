import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { getMe, type UserResponse } from "@/lib/auth";
import { UniversityLearningView } from "@/components/university/UniversityLearningView";
import { UniversityOnlyNotice } from "@/components/university/UniversityOnlyNotice";

export const Route = createFileRoute("/universidad/aprendizaje")({
  component: UniversityLearningPage,
});

function UniversityLearningPage() {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    getMe()
      .then(setUser)
      .catch(() => navigate({ to: "/cuenta" }))
      .finally(() => setLoading(false));
  }, [navigate]);

  if (loading) return <p className="mx-auto max-w-5xl px-6 py-16 animate-pulse text-muted-foreground">Cargando…</p>;
  if (!user) return null;
  if (user.role !== "UNIVERSITY_STUDENT" && user.role !== "ADMIN") {
    return <div className="mx-auto max-w-3xl px-6 py-12"><UniversityOnlyNotice /></div>;
  }
  return (
    <div className="mx-auto max-w-5xl px-6 py-12">
      <h1 className="font-display text-3xl font-semibold text-primary">Mi aprendizaje</h1>
      <p className="mt-2 text-muted-foreground">Materiales y tareas de tu curso.</p>
      <div className="mt-8"><UniversityLearningView /></div>
    </div>
  );
}
