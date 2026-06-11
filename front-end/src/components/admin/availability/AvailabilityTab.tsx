import { useState } from "react";
import type { BookingConflict } from "@/lib/admin";
import { WeeklyAvailabilityEditor } from "./WeeklyAvailabilityEditor";
import { BookingConflictNotice } from "./BookingConflictNotice";

export function AvailabilityTab() {
  const [conflicts, setConflicts] = useState<BookingConflict[]>([]);

  return (
    <div className="space-y-6">
      <p className="text-sm text-muted-foreground">
        Define cuándo estás disponible. Tus alumnos verán estas horas en la
        página de reservas.
      </p>
      <BookingConflictNotice conflicts={conflicts} />
      <WeeklyAvailabilityEditor onConflicts={setConflicts} />
    </div>
  );
}
