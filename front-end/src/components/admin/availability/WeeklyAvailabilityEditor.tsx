import { Fragment, useEffect, useMemo, useState } from "react";
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
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

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

const HOUR_START = 7;
const HOUR_END = 21;
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
  const [initialSelected, setInitialSelected] = useState<Set<string>>(
    new Set(),
  );
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
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
      `3rem ` +
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
    setInitialSelected(s);
  };

  useEffect(() => {
    getAvailability()
      .then(({ days }) => applyAvailability(days))
      .catch(() => setError(t("admin.availability.loadError")))
      .finally(() => setLoading(false));
  }, []);

  const toggle = (dateStr: string, hour: number) => {
    const key = `${dateStr}:${hour}`;
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
    setSaved(false);
  };

  const save = async () => {
    setSaving(true);
    setError(null);
    setSaved(false);
    try {
      const changedDates = new Set<string>();
      for (const col of dates) {
        for (const hour of HOURS) {
          const key = `${col.dateStr}:${hour}`;
          if (selected.has(key) !== initialSelected.has(key)) {
            changedDates.add(col.dateStr);
            break;
          }
        }
      }

      let conflicts: BookingConflict[] = [];
      for (const dateStr of changedDates) {
        const hours = HOURS.filter((h) => selected.has(`${dateStr}:${h}`));
        const res = await setDayAvailability(dateStr, hoursToWindows(hours));
        conflicts = res.bookingConflicts;
      }

      const fresh = await getAvailability();
      applyAvailability(fresh.days);
      onConflicts(conflicts);
      setSaved(true);
    } catch (e) {
      getAvailability()
        .then(({ days }) => applyAvailability(days))
        .catch(() => {});
      setError((e as ApiError).message ?? t("admin.availability.saveError"));
    } finally {
      setSaving(false);
    }
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
                <div className="flex items-center justify-end pr-2 text-xs text-muted-foreground h-11">
                  {String(hour).padStart(2, "0")}:00
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
        {saved && (
          <p className="text-sm text-green-600">
            {t("admin.availability.saved")}
          </p>
        )}

        <Button onClick={save} disabled={saving}>
          {saving
            ? t("admin.availability.saving")
            : t("admin.availability.save")}
        </Button>
      </CardContent>
    </Card>
  );
}
