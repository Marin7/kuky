import type { Slot } from "@/lib/scheduling";

interface SlotGridProps {
  slots: Slot[];
  onSelect: (slot: Slot) => void;
}

const DAY_NAMES = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"];

function formatTime(iso: string): string {
  return new Intl.DateTimeFormat(undefined, {
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(iso));
}

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat(undefined, {
    weekday: "short",
    day: "numeric",
    month: "short",
  }).format(new Date(iso));
}

function groupByDay(slots: Slot[]): Map<string, Slot[]> {
  const map = new Map<string, Slot[]>();
  for (const slot of slots) {
    const dayKey = new Date(slot.start).toLocaleDateString();
    if (!map.has(dayKey)) map.set(dayKey, []);
    map.get(dayKey)!.push(slot);
  }
  return map;
}

export function SlotGrid({ slots, onSelect }: SlotGridProps) {
  const byDay = groupByDay(slots);

  if (slots.length === 0) {
    return (
      <p className="text-center text-muted-foreground py-8">
        No hay horas disponibles para esta semana.
      </p>
    );
  }

  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-7 gap-3">
      {Array.from(byDay.entries()).map(([dayKey, daySlots]) => (
        <div key={dayKey} className="space-y-1">
          <p className="text-xs font-medium text-muted-foreground text-center">
            {formatDate(daySlots[0].start)}
          </p>
          {daySlots.map((slot) => {
            const isOpen = slot.status === "OPEN";
            return (
              <button
                key={slot.start}
                disabled={!isOpen}
                onClick={() => isOpen && onSelect(slot)}
                className={[
                  "w-full rounded-md px-2 py-1.5 text-xs font-medium transition-colors",
                  isOpen
                    ? "bg-primary text-primary-foreground hover:bg-primary/90 cursor-pointer"
                    : slot.status === "BOOKED"
                      ? "bg-muted text-muted-foreground cursor-not-allowed"
                      : "bg-muted/50 text-muted-foreground/50 cursor-not-allowed",
                ].join(" ")}
              >
                {formatTime(slot.start)}
                {slot.status === "BOOKED" && (
                  <span className="block text-[10px] opacity-70">
                    reservado
                  </span>
                )}
              </button>
            );
          })}
        </div>
      ))}
    </div>
  );
}
