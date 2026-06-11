import { useEffect, useState } from "react";
import { getAdminBookings, type AdminBooking } from "@/lib/admin";
import { Card, CardContent } from "@/components/ui/card";
import { StudentLink } from "@/components/admin/students/StudentLink";

function formatSlot(isoStart: string, isoEnd: string): string {
  const start = new Date(isoStart);
  const end = new Date(isoEnd);
  const datePart = new Intl.DateTimeFormat("es", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: "Europe/Madrid",
  }).format(start);
  const timePart = new Intl.DateTimeFormat("es", {
    hour: "2-digit",
    minute: "2-digit",
    timeZone: "Europe/Madrid",
  }).format(start);
  const endTime = new Intl.DateTimeFormat("es", {
    hour: "2-digit",
    minute: "2-digit",
    timeZone: "Europe/Madrid",
  }).format(end);
  return `${datePart}, ${timePart}–${endTime}`;
}

export function BookingsTab() {
  const [bookings, setBookings] = useState<AdminBooking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getAdminBookings()
      .then(setBookings)
      .catch(() => setError("No se pudieron cargar las reservas."))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <p className="text-sm text-muted-foreground animate-pulse">Cargando…</p>
    );
  }

  if (error) {
    return <p className="text-sm text-destructive">{error}</p>;
  }

  if (bookings.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        No hay clases confirmadas próximas.
      </p>
    );
  }

  return (
    <div className="space-y-3">
      <p className="text-sm text-muted-foreground">
        Clases confirmadas a partir de ahora, ordenadas por fecha.
      </p>
      {bookings.map((b) => (
        <Card key={b.id} className="text-sm">
          <CardContent className="pt-4 space-y-1">
            <p className="font-medium capitalize">
              {formatSlot(b.slotStart, b.slotEnd)}
            </p>
            <StudentLink
              student={{
                id: b.studentId,
                email: b.studentEmail,
                firstName: b.studentFirstName,
                lastName: b.studentLastName,
                username: b.studentUsername,
              }}
              showEmail
            />
            {b.zoomJoinUrl && (
              <a
                href={b.zoomJoinUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary text-xs break-all hover:underline"
              >
                {b.zoomJoinUrl}
              </a>
            )}
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
