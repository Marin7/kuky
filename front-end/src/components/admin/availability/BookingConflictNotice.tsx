import type { BookingConflict } from "@/lib/admin";

function formatSlot(iso: string): string {
  return new Intl.DateTimeFormat("es", {
    weekday: "long",
    day: "numeric",
    month: "long",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(iso));
}

interface Props {
  conflicts: BookingConflict[];
}

/**
 * Non-blocking warning: confirmed bookings that now fall outside saved availability.
 * The bookings are preserved — this only alerts the teacher.
 */
export function BookingConflictNotice({ conflicts }: Props) {
  if (conflicts.length === 0) return null;
  return (
    <div className="rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">
      <p className="font-medium">
        Atención: hay {conflicts.length} reserva(s) confirmada(s) fuera de tu
        nueva disponibilidad.
      </p>
      <p className="mt-1 text-xs">
        Estas clases se mantienen y no se han cancelado. Contacta con tus
        alumnos si necesitas reprogramarlas:
      </p>
      <ul className="mt-2 space-y-1">
        {conflicts.map((c) => (
          <li key={c.bookingId} className="text-xs">
            {c.studentEmail} · {formatSlot(c.slotStart)}
          </li>
        ))}
      </ul>
    </div>
  );
}
