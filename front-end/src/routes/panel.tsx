import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { getMe, type UserResponse } from "@/lib/auth";
import { AdminPanel } from "@/components/admin/AdminPanel";

export const Route = createFileRoute("/panel")({
  head: () => ({
    meta: [
      { title: "Panel de control — Español con Paula" },
      {
        name: "description",
        content:
          "Panel de la profesora: disponibilidad, tareas y presentaciones.",
      },
    ],
  }),
  component: PanelPage,
});

function PanelPage() {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    getMe()
      .then((me) => {
        if (me.role !== "ADMIN") {
          // Logged-in students never see the panel.
          navigate({ to: "/" });
          return;
        }
        setUser(me);
      })
      .catch(() => {
        setUser(null);
        // Guests are sent to sign in.
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

  return <AdminPanel />;
}
