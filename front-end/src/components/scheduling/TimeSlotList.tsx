import type { Slot } from "@/lib/scheduling";

function toLocalDateKey(iso: string): string {
  return new Intl.DateTimeFormat("sv-SE").format(new Date(iso));
}

function formatDayHeading(dateKey: string): string {
  return new Intl.DateTimeFormat("es", {
    weekday: "long",
    day: "numeric",
    month: "long",
  }).format(new Date(dateKey + "T12:00:00"));
}

function formatTime(iso: string): string {
  return new Intl.DateTimeFormat("es", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(iso));
}

interface TimeSlotListProps {
  slots: Slot[];
  selectedDay: string | null;
  onSelect: (slot: Slot) => void;
}

export function TimeSlotList({
  slots,
  selectedDay,
  onSelect,
}: TimeSlotListProps) {
  if (!selectedDay) {
    return (
      <div className="flex items-center justify-center h-full min-h-[140px] text-sm text-muted-foreground text-center px-4">
        Selecciona un día para ver los horarios disponibles.
      </div>
    );
  }

  const daySlots = slots.filter(
    (s) => toLocalDateKey(s.start) === selectedDay && s.status === "OPEN",
  );

  const heading = formatDayHeading(selectedDay);

  return (
    <div className="space-y-3">
      <p className="text-sm font-semibold capitalize">{heading}</p>

      {daySlots.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          No hay horarios disponibles para este día.
        </p>
      ) : (
        <div className="flex flex-col gap-2">
          {daySlots.map((slot) => (
            <button
              key={slot.start}
              onClick={() => onSelect(slot)}
              className="w-full rounded-lg border border-primary/30 px-4 py-2 text-sm font-medium text-foreground hover:bg-primary/10 hover:border-primary transition-colors text-left"
            >
              {formatTime(slot.start)}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
