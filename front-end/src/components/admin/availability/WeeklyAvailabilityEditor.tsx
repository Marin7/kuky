import { Fragment, useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getAvailability,
  setDayAvailability,
  type DayAvailability,
  type DayWindow,
  type BookingConflict,
  type ApiError,
} from "@/lib/admin";
import { useTeacherTimezone } from "@/hooks/useTeacherTimezone";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

// Debounce per-date saves so rapid clicks on the same day only trigger one PUT request.
const AUTOSAVE_DEBOUNCE_MS = 600;

const DAY_ABBREVS = ["", "lun", "mar", "mié", "jue", "vie", "sáb", "dom"];
const MONTH_ABBREVS = [
  "ene",
  "feb",
  "mar",
  "abr",
  "may",
  "jun",
  "jul",
  "ago",
  "sep",
  "oct",
  "nov",
  "dic",
];

const WEEKS = 4;
const DAYS = WEEKS * 7;

const HOUR_START = 8;
const HOUR_END = 20;
const HOURS = Array.from(
  { length: HOUR_END - HOUR_START },
  (_, i) => HOUR_START + i,
);

interface DateCol {
  dow: number;
  dateStr: string;
  dayLabel: string;
  dayNum: number;
  monthLabel: string;
}

// Dates are anchored to the teacher's working time zone (not the admin's device zone,
// per FR-007), then iterated with UTC-based Date methods to avoid the browser's own
// local zone shifting the calendar day — the same pattern CalendarPicker.tsx uses.
function todayKeyInZone(zone: string): string {
  return new Intl.DateTimeFormat("sv-SE", { timeZone: zone }).format(
    new Date(),
  );
}

function formatDateStr(date: Date): string {
  return new Intl.DateTimeFormat("sv-SE", { timeZone: "UTC" }).format(date);
}

function getHorizonDates(zone: string): DateCol[] {
  const anchor = new Date(todayKeyInZone(zone) + "T00:00:00Z");
  const dayJs = anchor.getUTCDay();
  const daysToMonday = dayJs === 0 ? 6 : dayJs - 1;
  const monday = new Date(anchor);
  monday.setUTCDate(anchor.getUTCDate() - daysToMonday);

  return Array.from({ length: DAYS }, (_, i) => {
    const d = new Date(monday);
    d.setUTCDate(monday.getUTCDate() + i);
    const dow = d.getUTCDay() === 0 ? 7 : d.getUTCDay();
    return {
      dow,
      dateStr: formatDateStr(d),
      dayLabel: DAY_ABBREVS[dow],
      dayNum: d.getUTCDate(),
      monthLabel: MONTH_ABBREVS[d.getUTCMonth()],
    };
  });
}

function computeSelected(
  days: DayAvailability[],
  dates: DateCol[],
): Set<string> {
  const byDate = new Map(days.map((d) => [d.date, d.windows]));
  const selected = new Set<string>();
  for (const col of dates) {
    const windows = byDate.get(col.dateStr) ?? [];
    for (const hour of HOURS) {
      const on = windows.some((w) => {
        const s = parseInt(w.startTime.split(":")[0]);
        const e = parseInt(w.endTime.split(":")[0]);
        return hour >= s && hour < e;
      });
      if (on) selected.add(`${col.dateStr}:${hour}`);
    }
  }
  return selected;
}

/** Contiguous selected hours for a date → absolute windows. */
function hoursToWindows(hours: number[]): DayWindow[] {
  if (hours.length === 0) return [];
  const sorted = [...hours].sort((a, b) => a - b);
  const windows: DayWindow[] = [];
  let start = sorted[0];
  let prev = sorted[0];
  for (let i = 1; i <= sorted.length; i++) {
    const cur = sorted[i];
    if (cur !== prev + 1) {
      windows.push({
        startTime: `${String(start).padStart(2, "0")}:00`,
        endTime: `${String(prev + 1).padStart(2, "0")}:00`,
      });
      if (cur !== undefined) start = cur;
    }
    if (cur !== undefined) prev = cur;
  }
  return windows;
}

interface Props {
  onConflicts: (conflicts: BookingConflict[]) => void;
}

export function WeeklyAvailabilityEditor({ onConflicts }: Props) {
  const { t } = useTranslation();
  const teacherTimezone = useTeacherTimezone();
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [savingDates, setSavingDates] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  // Mirrors `selected` synchronously so debounced saves always read the latest value,
  // instead of a value captured by a stale closure from an earlier render.
  const latestSelectedRef = useRef<Set<string>>(new Set());
  const saveTimers = useRef<Map<string, ReturnType<typeof setTimeout>>>(
    new Map(),
  );
  const dates = useMemo(
    () => getHorizonDates(teacherTimezone),
    [teacherTimezone],
  );
  const todayStr = useMemo(
    () => todayKeyInZone(teacherTimezone),
    [teacherTimezone],
  );
  const weeks = useMemo(
    () =>
      Array.from({ length: WEEKS }, (_, w) => dates.slice(w * 7, w * 7 + 7)),
    [dates],
  );
  const gridTemplateColumns = useMemo(
    () =>
      `5rem ` +
      weeks
        .map(
          (_, w) =>
            `repeat(7, minmax(44px, 1fr))${w < WEEKS - 1 ? " 14px" : ""}`,
        )
        .join(" "),
    [weeks],
  );

  const applyAvailability = (days: DayAvailability[]) => {
    const s = computeSelected(days, dates);
    setSelected(s);
    latestSelectedRef.current = s;
  };

  useEffect(() => {
    getAvailability()
      .then(({ days }) => applyAvailability(days))
      .catch(() => setError(t("admin.availability.loadError")))
      .finally(() => setLoading(false));
  }, []);

  // Cancel any pending debounced saves on unmount so they don't fire against a gone component.
  useEffect(() => {
    return () => {
      saveTimers.current.forEach((timer) => clearTimeout(timer));
    };
  }, []);

  const scheduleSave = (dateStr: string) => {
    const existing = saveTimers.current.get(dateStr);
    if (existing) clearTimeout(existing);

    const timer = setTimeout(async () => {
      saveTimers.current.delete(dateStr);
      setSaved(false);
      setError(null);
      setSavingDates((prev) => new Set(prev).add(dateStr));
      try {
        const hours = HOURS.filter((h) =>
          latestSelectedRef.current.has(`${dateStr}:${h}`),
        );
        const res = await setDayAvailability(dateStr, hoursToWindows(hours));
        onConflicts(res.bookingConflicts);
        setSaved(true);
      } catch (e) {
        setError((e as ApiError).message ?? t("admin.availability.saveError"));
        // Resync with the server so the grid doesn't keep showing a change that failed to persist.
        getAvailability()
          .then(({ days }) => applyAvailability(days))
          .catch(() => {});
      } finally {
        setSavingDates((prev) => {
          const next = new Set(prev);
          next.delete(dateStr);
          return next;
        });
      }
    }, AUTOSAVE_DEBOUNCE_MS);
    saveTimers.current.set(dateStr, timer);
  };

  const toggle = (dateStr: string, hour: number) => {
    const key = `${dateStr}:${hour}`;
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      latestSelectedRef.current = next;
      return next;
    });
    setSaved(false);
    scheduleSave(dateStr);
  };

  if (loading)
    return (
      <p className="text-sm text-muted-foreground">
        {t("admin.availability.loading")}
      </p>
    );

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">
          {t("admin.availability.perWeek.title")}
        </CardTitle>
        <p className="text-sm text-muted-foreground">
          {t("admin.availability.perWeek.description")}
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="overflow-x-auto">
          <div className="inline-grid gap-y-1" style={{ gridTemplateColumns }}>
            {/* Header row */}
            <div />
            {weeks.map((week, w) => (
              <Fragment key={`h-${w}`}>
                {week.map((d) => (
                  <div
                    key={d.dateStr}
                    className={`text-center text-xs pb-2 px-0.5 ${d.dateStr < todayStr ? "opacity-40" : ""}`}
                  >
                    <div className="font-medium capitalize">{d.dayLabel}</div>
                    <div className="text-muted-foreground">
                      {d.dayNum} {d.monthLabel}
                    </div>
                  </div>
                ))}
                {w < WEEKS - 1 && <div />}
              </Fragment>
            ))}

            {/* Hour rows */}
            {HOURS.map((hour) => (
              <Fragment key={hour}>
                <div className="flex items-center justify-end pr-2 text-xs text-muted-foreground h-11 whitespace-nowrap">
                  {String(hour).padStart(2, "0")}:00-
                  {String(hour + 1).padStart(2, "0")}:00
                </div>
                {weeks.map((week, w) => (
                  <Fragment key={`${hour}-${w}`}>
                    {week.map((d) => {
                      const key = `${d.dateStr}:${hour}`;
                      const isOn = selected.has(key);
                      return (
                        <button
                          key={d.dateStr}
                          onClick={() => toggle(d.dateStr, hour)}
                          className={`h-11 mx-0.5 rounded border transition-colors ${
                            d.dateStr < todayStr
                              ? `opacity-30 cursor-default pointer-events-none ${isOn ? "bg-primary/15 border-primary/40" : "border-border/30"}`
                              : isOn
                                ? "bg-primary/15 border-primary/60 hover:bg-primary/25"
                                : "border-border/40 hover:bg-muted/50"
                          }`}
                        />
                      );
                    })}
                    {w < WEEKS - 1 && (
                      <div className="border-l border-border/30 mx-1" />
                    )}
                  </Fragment>
                ))}
              </Fragment>
            ))}
          </div>
        </div>

        {error && <p className="text-sm text-destructive">{error}</p>}
        {savingDates.size > 0 ? (
          <p className="text-sm text-muted-foreground">
            {t("admin.availability.saving")}
          </p>
        ) : (
          saved && (
            <p className="text-sm text-green-600">
              {t("admin.availability.saved")}
            </p>
          )
        )}
      </CardContent>
    </Card>
  );
}
