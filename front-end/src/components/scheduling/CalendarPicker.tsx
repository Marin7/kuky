import { useMemo } from "react";
import type { Slot } from "@/lib/scheduling";

function toLocalDateKey(iso: string): string {
  return new Intl.DateTimeFormat("sv-SE").format(new Date(iso));
}

function formatMonthYear(dateKey: string): string {
  return new Intl.DateTimeFormat("es", {
    month: "long",
    year: "numeric",
  }).format(new Date(dateKey + "T12:00:00"));
}

interface DayCellProps {
  day: string;
  available: boolean;
  selected: boolean;
  isToday: boolean;
  onSelect: (day: string) => void;
}

function DayCell({
  day,
  available,
  selected,
  isToday,
  onSelect,
}: DayCellProps) {
  const dayNumber = parseInt(day.split("-")[2], 10);

  if (!available) {
    return (
      <div className="flex items-center justify-center h-10 w-10 mx-auto rounded-full text-sm text-muted-foreground/40 select-none">
        {dayNumber}
      </div>
    );
  }

  return (
    <button
      onClick={() => onSelect(day)}
      className={[
        "flex items-center justify-center h-10 w-10 mx-auto rounded-full text-sm font-medium transition-colors",
        selected
          ? "bg-primary text-primary-foreground"
          : isToday
            ? "ring-2 ring-primary text-primary hover:bg-primary/10"
            : "hover:bg-primary/10 text-foreground",
      ].join(" ")}
    >
      {dayNumber}
    </button>
  );
}

interface CalendarPickerProps {
  slots: Slot[];
  horizonStart: string;
  horizonEnd: string;
  selectedDay: string | null;
  onSelectDay: (day: string) => void;
}

export function CalendarPicker({
  slots,
  horizonStart,
  horizonEnd,
  selectedDay,
  onSelectDay,
}: CalendarPickerProps) {
  const todayKey = toLocalDateKey(new Date().toISOString());

  const availableDays = useMemo(() => {
    const days = new Set<string>();
    for (const slot of slots) {
      if (slot.status === "OPEN") {
        days.add(toLocalDateKey(slot.start));
      }
    }
    return days;
  }, [slots]);

  const allDays = useMemo(() => {
    const result: string[] = [];
    const cursor = new Date(horizonStart);
    const end = new Date(horizonEnd);
    while (cursor < end) {
      result.push(toLocalDateKey(cursor.toISOString()));
      cursor.setUTCDate(cursor.getUTCDate() + 1);
    }
    return result;
  }, [horizonStart, horizonEnd]);

  const week1 = allDays.slice(0, 7);
  const week2 = allDays.slice(7, 14);

  const month1 = allDays[0] ? formatMonthYear(allDays[0]) : "";
  const month2 = allDays[13] ? formatMonthYear(allDays[13]) : "";
  const monthLabel = month1 === month2 ? month1 : `${month1} – ${month2}`;

  const DAY_HEADERS = ["Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do"];

  const renderWeek = (days: string[]) => (
    <div className="grid grid-cols-7 gap-1">
      {days.map((day) => (
        <DayCell
          key={day}
          day={day}
          available={availableDays.has(day)}
          selected={selectedDay === day}
          isToday={day === todayKey}
          onSelect={onSelectDay}
        />
      ))}
    </div>
  );

  return (
    <div className="space-y-3 select-none min-w-[280px]">
      <p className="text-sm font-semibold capitalize text-center">
        {monthLabel}
      </p>

      <div className="grid grid-cols-7 gap-1">
        {DAY_HEADERS.map((d) => (
          <div
            key={d}
            className="flex items-center justify-center h-8 text-xs font-medium text-muted-foreground"
          >
            {d}
          </div>
        ))}
      </div>

      {renderWeek(week1)}
      {renderWeek(week2)}
    </div>
  );
}
