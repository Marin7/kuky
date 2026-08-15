import { createFileRoute, Navigate } from "@tanstack/react-router";

export const Route = createFileRoute("/prueba-de-nivel")({
  component: () => <Navigate to="/quizzes" />,
});
