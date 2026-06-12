import { useTranslation } from "react-i18next";
import type { Slot } from "@/lib/scheduling";

function toLocalDateKey(iso: string, timezone: string): string {
  return new Intl.DateTimeFormat("sv-SE", { timeZone: timezone }).format(
    new Date(iso),
  );
}

function formatDayHeading(dateKey: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, {
    weekday: "long",
    day: "numeric",
    month: "long",
  }).format(new Date(dateKey + "T12:00:00"));
}

function formatTime(iso: string, timezone: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, {
    hour: "2-digit",
    minute: "2-digit",
    timeZone: timezone,
  }).format(new Date(iso));
}

interface TimeSlotListProps {
  slots: Slot[];
  selectedDay: string | null;
  timezone: string;
  onSelect: (slot: Slot) => void;
}

export function TimeSlotList({
  slots,
  selectedDay,
  timezone,
  onSelect,
}: TimeSlotListProps) {
  const { t, i18n } = useTranslation();

  if (!selectedDay) {
    return (
      <div className="flex items-center justify-center h-full min-h-[140px] text-sm text-muted-foreground text-center px-4">
        {t("schedule.selectDay")}
      </div>
    );
  }

  const daySlots = slots.filter(
    (s) =>
      toLocalDateKey(s.start, timezone) === selectedDay && s.status === "OPEN",
  );

  const heading = formatDayHeading(selectedDay, i18n.language);

  return (
    <div className="space-y-3">
      <p className="text-sm font-semibold capitalize">{heading}</p>

      {daySlots.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {t("schedule.noSlotsDay")}
        </p>
      ) : (
        <div className="flex flex-col gap-2">
          {daySlots.map((slot) => (
            <button
              key={slot.start}
              onClick={() => onSelect(slot)}
              className="w-full rounded-lg border border-primary/30 px-4 py-2 text-sm font-medium text-foreground hover:bg-primary/10 hover:border-primary transition-colors text-left"
            >
              {formatTime(slot.start, timezone, i18n.language)}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
