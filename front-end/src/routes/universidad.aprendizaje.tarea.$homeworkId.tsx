import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { getMe } from "@/lib/auth";
import { UniversityHomeworkExercisePage } from "@/components/university/UniversityHomeworkExercisePage";
import { UniversityOnlyNotice } from "@/components/university/UniversityOnlyNotice";

export const Route = createFileRoute("/universidad/aprendizaje/tarea/$homeworkId")({
  component: UniversityExerciseRoute,
});

function UniversityExerciseRoute() {
  const { homeworkId } = Route.useParams();
  const [allowed, setAllowed] = useState<boolean | null>(null);
  const navigate = useNavigate();
  useEffect(() => {
    getMe()
      .then((user) => setAllowed(user.role === "UNIVERSITY_STUDENT" || user.role === "ADMIN"))
      .catch(() => navigate({ to: "/cuenta" }));
  }, [navigate]);
  if (allowed === null) return <p className="mx-auto max-w-2xl px-6 py-16 animate-pulse text-muted-foreground">Cargando…</p>;
  if (!allowed) return <div className="mx-auto max-w-3xl px-6 py-12"><UniversityOnlyNotice /></div>;
  return <UniversityHomeworkExercisePage homeworkId={homeworkId} />;
}
