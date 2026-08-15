import { createFileRoute, Navigate } from "@tanstack/react-router";

export const Route = createFileRoute("/quizzes")({
  component: () => <Navigate to="/aprendizaje" />,
});
