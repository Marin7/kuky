import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMe, type UserResponse } from "@/lib/auth";
import { useTimezone } from "@/hooks/useTimezone";
import { ScheduleView } from "@/components/scheduling/ScheduleView";
import { MyBookings } from "@/components/scheduling/MyBookings";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/reservas")({
  head: () => ({
    meta: seo({
      title: "Reservas — Destino: Español",
      description:
        "Consulta el horario disponible y reserva tu clase de español.",
      path: "/reservas",
    }),
  }),
  component: ReservasPage,
});

function ReservasPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [user, setUser] = useState<UserResponse | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const { zone } = useTimezone();
  const scheduleRefreshRef = useRef<(() => void) | null>(null);
  const myBookingsRefreshRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    getMe()
      .then(setUser)
      .catch(() => {
        setUser(null);
        navigate({ to: "/cuenta" });
      })
      .finally(() => setAuthLoading(false));
  }, [navigate]);

  if (authLoading) {
    return (
      <div className="mx-auto max-w-5xl px-6 py-16 text-center">
        <p className="text-muted-foreground text-sm animate-pulse">
          {t("common.loading")}
        </p>
      </div>
    );
  }

  if (!user) return null;

  return (
    <div>
      <ScheduleView
        timezone={zone}
        onRefreshRef={scheduleRefreshRef}
        onBookingSuccess={() => myBookingsRefreshRef.current?.()}
      />
      <MyBookings
        timezone={zone}
        onRefreshRef={myBookingsRefreshRef}
        onScheduleRefresh={() => scheduleRefreshRef.current?.()}
      />
    </div>
  );
}
