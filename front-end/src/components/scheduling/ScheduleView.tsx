import { useEffect, useState } from "react";
import {
  getSchedule,
  type Slot,
  type ScheduleResponse,
} from "@/lib/scheduling";
import { getMe, type UserResponse } from "@/lib/auth";
import { Skeleton } from "@/components/ui/skeleton";
import { CalendarPicker } from "./CalendarPicker";
import { TimeSlotList } from "./TimeSlotList";
import { BookingDialog } from "./BookingDialog";

// Mirrors the loaded two-panel card (calendar grid + slot list) so the layout
// doesn't shift when data arrives.
function ScheduleSkeleton() {
  return (
    <div className="rounded-xl border bg-card p-6 shadow-sm">
      <div className="flex flex-col md:flex-row gap-6 md:gap-10">
        <div className="space-y-3">
          <Skeleton className="h-5 w-40" />
          <div className="grid grid-cols-7 gap-2">
            {Array.from({ length: 21 }).map((_, i) => (
              <Skeleton key={i} className="h-9 w-9 rounded-lg" />
            ))}
          </div>
        </div>

        <div className="hidden md:block w-px bg-border shrink-0" />
        <hr className="md:hidden" />

        <div className="flex-1 min-w-0 space-y-3">
          <Skeleton className="h-5 w-48" />
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full rounded-lg" />
          ))}
        </div>
      </div>
    </div>
  );
}

interface ScheduleViewProps {
  onRefreshRef?: React.MutableRefObject<(() => void) | null>;
  onBookingSuccess?: () => void;
  onTimezoneResolved?: (tz: string) => void;
}

export function ScheduleView({
  onRefreshRef,
  onBookingSuccess,
  onTimezoneResolved,
}: ScheduleViewProps) {
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
      .then((s) => {
        setSchedule(s);
        onTimezoneResolved?.(s.teacherTimezone);
      })
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
          Selecciona un día y una hora disponible para reservar tu clase. Los
          horarios se muestran en horario de Madrid (España).
        </p>
      </div>

      {loading && <ScheduleSkeleton />}

      {error && (
        <div className="py-16 text-center text-destructive text-sm">
          {error}
        </div>
      )}

      {!loading && !error && schedule && (
        <div className="rounded-xl border bg-card p-6 shadow-sm">
          <div className="flex flex-col md:flex-row gap-6 md:gap-10">
            <CalendarPicker
              slots={schedule.slots}
              horizonStart={schedule.horizonStart}
              horizonEnd={schedule.horizonEnd}
              timezone={schedule.teacherTimezone}
              selectedDay={selectedDay}
              onSelectDay={setSelectedDay}
            />

            <div className="hidden md:block w-px bg-border shrink-0" />
            <hr className="md:hidden" />

            <div className="flex-1 min-w-0">
              <TimeSlotList
                slots={schedule.slots}
                selectedDay={selectedDay}
                timezone={schedule.teacherTimezone}
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
