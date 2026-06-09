import { useEffect, useState } from "react";
import { getSchedule, type Slot, type ScheduleResponse } from "@/lib/scheduling";
import { getMe, type UserResponse } from "@/lib/auth";
import { CalendarPicker } from "./CalendarPicker";
import { TimeSlotList } from "./TimeSlotList";
import { BookingDialog } from "./BookingDialog";

interface ScheduleViewProps {
  onRefreshRef?: React.MutableRefObject<(() => void) | null>;
  onBookingSuccess?: () => void;
}

export function ScheduleView({ onRefreshRef, onBookingSuccess }: ScheduleViewProps) {
  const [schedule, setSchedule] = useState<ScheduleResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedDay, setSelectedDay] = useState<string | null>(null);
  const [selectedSlot, setSelectedSlot] = useState<Slot | null>(null);
  const [user, setUser] = useState<UserResponse | null>(null);

  const fetchSchedule = () => {
    setLoading(true);
    setError(null);
    getSchedule()
      .then(setSchedule)
      .catch(() =>
        setError("No se pudo cargar el horario. Inténtalo de nuevo más tarde."),
      )
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchSchedule();
    getMe()
      .then(setUser)
      .catch(() => setUser(null));

    if (onRefreshRef) {
      onRefreshRef.current = fetchSchedule;
    }
  }, []);

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 space-y-6">
      <div>
        <h1 className="font-display text-2xl font-bold">Horario de clases</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Selecciona un día y una hora disponible para reservar tu clase.
          Los horarios se muestran en tu zona horaria local.
        </p>
      </div>

      {loading && (
        <div className="py-16 text-center text-muted-foreground text-sm">
          Cargando horario…
        </div>
      )}

      {error && (
        <div className="py-16 text-center text-destructive text-sm">{error}</div>
      )}

      {!loading && !error && schedule && (
        <div className="rounded-xl border bg-card p-6 shadow-sm">
          <div className="flex flex-col md:flex-row gap-6 md:gap-10">
            <CalendarPicker
              slots={schedule.slots}
              horizonStart={schedule.horizonStart}
              horizonEnd={schedule.horizonEnd}
              selectedDay={selectedDay}
              onSelectDay={setSelectedDay}
            />

            <div className="hidden md:block w-px bg-border shrink-0" />
            <hr className="md:hidden" />

            <div className="flex-1 min-w-0">
              <TimeSlotList
                slots={schedule.slots}
                selectedDay={selectedDay}
                onSelect={setSelectedSlot}
              />
            </div>
          </div>
        </div>
      )}

      <BookingDialog
        slot={selectedSlot}
        isAuthenticated={!!user}
        onClose={() => setSelectedSlot(null)}
        onSuccess={() => {
          fetchSchedule();
          onBookingSuccess?.();
        }}
      />
    </div>
  );
}
