import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useRef, useState } from "react";
import { getMe, type UserResponse } from "@/lib/auth";
import { ScheduleView } from "@/components/scheduling/ScheduleView";
import { MyBookings } from "@/components/scheduling/MyBookings";

export const Route = createFileRoute("/reservas")({
  head: () => ({
    meta: [
      { title: "Reservas — Español con Paula" },
      {
        name: "description",
        content:
          "Consulta el horario disponible y reserva tu clase de español.",
      },
    ],
  }),
  component: ReservasPage,
});

function ReservasPage() {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const scheduleRefreshRef = useRef<(() => void) | null>(null);
  const myBookingsRefreshRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    getMe()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setAuthLoading(false));
  }, []);

  return (
    <div>
      <ScheduleView
        onRefreshRef={scheduleRefreshRef}
        onBookingSuccess={() => myBookingsRefreshRef.current?.()}
      />
      {!authLoading && user && (
        <MyBookings
          onRefreshRef={myBookingsRefreshRef}
          onScheduleRefresh={() => scheduleRefreshRef.current?.()}
        />
      )}
    </div>
  );
}
