import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useRef, useState } from "react";
import { getMe, type UserResponse } from "@/lib/auth";
import { ScheduleView } from "@/components/scheduling/ScheduleView";
import { MyBookings } from "@/components/scheduling/MyBookings";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/reservas")({
  head: () => ({
    meta: seo({
      title: "Reservas — Español con Paula",
      description:
        "Consulta el horario disponible y reserva tu clase de español.",
      path: "/reservas",
    }),
  }),
  component: ReservasPage,
});

function ReservasPage() {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [teacherTimezone, setTeacherTimezone] = useState("Europe/Madrid");
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
        onTimezoneResolved={setTeacherTimezone}
      />
      {!authLoading && user && (
        <MyBookings
          timezone={teacherTimezone}
          onRefreshRef={myBookingsRefreshRef}
          onScheduleRefresh={() => scheduleRefreshRef.current?.()}
        />
      )}
    </div>
  );
}
