import { Fragment, useEffect, useMemo, useState } from "react";
import {
  getAvailability,
  addException,
  deleteException,
  type WeeklyWindow,
  type AvailabilityException,
  type BookingConflict,
  type ApiError,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const DAY_ABBREVS = ["", "lun", "mar", "mié", "jue", "vie", "sáb", "dom"];
const MONTH_ABBREVS = [
  "ene", "feb", "mar", "abr", "may", "jun",
  "jul", "ago", "sep", "oct", "nov", "dic",
];

const HOUR_START = 7;
const HOUR_END = 21;
const HOURS = Array.from(
  { length: HOUR_END - HOUR_START },
  (_, i) => HOUR_START + i,
);

interface DateCol {
  dow: number;      // 1=Mon … 7=Sun
  dateStr: string;  // "YYYY-MM-DD"
  dayLabel: string;
  dayNum: number;
  monthLabel: string;
}

function formatDateStr(date: Date): string {
  return new Intl.DateTimeFormat("sv-SE").format(date);
}

function getTwoWeekDates(): DateCol[] {
  const today = new Date();
  const dayJs = today.getDay();
  const daysToMonday = dayJs === 0 ? 6 : dayJs - 1;
  const monday = new Date(today);
  monday.setDate(today.getDate() - daysToMonday);
  monday.setHours(12, 0, 0, 0);

  return Array.from({ length: 14 }, (_, i) => {
    const d = new Date(monday);
    d.setDate(monday.getDate() + i);
    const dow = d.getDay() === 0 ? 7 : d.getDay();
    return {
      dow,
      dateStr: formatDateStr(d),
      dayLabel: DAY_ABBREVS[dow],
      dayNum: d.getDate(),
      monthLabel: MONTH_ABBREVS[d.getMonth()],
    };
  });
}

function isHourEnabledByRule(
  weekly: WeeklyWindow[],
  dow: number,
  hour: number,
): boolean {
  return weekly.some((w) => {
    if (w.dayOfWeek !== dow) return false;
    const s = parseInt(w.startTime.split(":")[0]);
    const e = parseInt(w.endTime.split(":")[0]);
    return hour >= s && hour < e;
  });
}

function computeSelected(
  weekly: WeeklyWindow[],
  exceptions: AvailabilityException[],
  dates: DateCol[],
): Set<string> {
  const selected = new Set<string>();
  for (const col of dates) {
    for (const hour of HOURS) {
      const ex = exceptions.find((e) => {
        if (e.date !== col.dateStr) return false;
        const s = parseInt(e.startTime.split(":")[0]);
        const en = parseInt(e.endTime.split(":")[0]);
        return hour >= s && hour < en;
      });
      const on = ex
        ? ex.kind === "OPEN"
        : isHourEnabledByRule(weekly, col.dow, hour);
      if (on) selected.add(`${col.dateStr}:${hour}`);
    }
  }
  return selected;
}

interface HourEntry {
  hour: number;
  kind: "OPEN" | "BLOCK";
}

function hoursToRanges(
  entries: HourEntry[],
): { kind: "OPEN" | "BLOCK"; startTime: string; endTime: string }[] {
  if (entries.length === 0) return [];
  entries.sort((a, b) => a.hour - b.hour);
  const ranges: { kind: "OPEN" | "BLOCK"; startTime: string; endTime: string }[] = [];
  let start = entries[0].hour;
  let prev = entries[0].hour;
  let kind = entries[0].kind;
  for (let i = 1; i <= entries.length; i++) {
    const cur = entries[i];
    if (!cur || cur.hour !== prev + 1 || cur.kind !== kind) {
      ranges.push({
        kind,
        startTime: `${String(start).padStart(2, "0")}:00`,
        endTime: `${String(prev + 1).padStart(2, "0")}:00`,
      });
      if (cur) { start = cur.hour; kind = cur.kind; }
    }
    if (cur) prev = cur.hour;
  }
  return ranges;
}

interface Props {
  onConflicts: (conflicts: BookingConflict[]) => void;
}

export function WeeklyAvailabilityEditor({ onConflicts }: Props) {
  const [weekly, setWeekly] = useState<WeeklyWindow[]>([]);
  const [serverExceptions, setServerExceptions] = useState<AvailabilityException[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [initialSelected, setInitialSelected] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const dates = useMemo(() => getTwoWeekDates(), []);
  const todayStr = useMemo(() => formatDateStr(new Date()), []);
  const week1 = dates.slice(0, 7);
  const week2 = dates.slice(7, 14);

  const applyAvailability = (w: WeeklyWindow[], ex: AvailabilityException[]) => {
    setWeekly(w);
    setServerExceptions(ex);
    const s = computeSelected(w, ex, dates);
    setSelected(s);
    setInitialSelected(s);
  };

  useEffect(() => {
    getAvailability()
      .then(({ weekly: w, exceptions: ex }) => applyAvailability(w, ex))
      .catch(() => setError("No se pudo cargar la disponibilidad."))
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
      // Collect dates with at least one changed cell
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

      for (const dateStr of changedDates) {
        const col = dates.find((d) => d.dateStr === dateStr)!;

        // Remove all existing exceptions for this date
        const toDelete = serverExceptions.filter((e) => e.date === dateStr);
        await Promise.all(toDelete.map((e) => deleteException(e.id)));

        // Hours where desired != weekly base → need an exception
        const exHours: HourEntry[] = [];
        for (const hour of HOURS) {
          const desired = selected.has(`${dateStr}:${hour}`);
          const rule = isHourEnabledByRule(weekly, col.dow, hour);
          if (desired !== rule) {
            exHours.push({ hour, kind: desired ? "OPEN" : "BLOCK" });
          }
        }

        // Merge consecutive same-kind hours into ranges and save
        const ranges = hoursToRanges(exHours);
        await Promise.all(
          ranges.map((r) => addException(dateStr, r.kind, r.startTime, r.endTime)),
        );
      }

      const fresh = await getAvailability();
      applyAvailability(fresh.weekly, fresh.exceptions);
      onConflicts([]);
      setSaved(true);
    } catch (e) {
      // Reload to show actual server state after partial failure
      getAvailability()
        .then(({ weekly: w, exceptions: ex }) => applyAvailability(w, ex))
        .catch(() => {});
      setError((e as ApiError).message ?? "No se pudo guardar.");
    } finally {
      setSaving(false);
    }
  };

  if (loading)
    return <p className="text-sm text-muted-foreground">Cargando…</p>;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Horario semanal</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="overflow-x-auto">
          <div
            className="inline-grid gap-y-1"
            style={{
              gridTemplateColumns: `3rem repeat(7, minmax(44px, 1fr)) 14px repeat(7, minmax(44px, 1fr))`,
            }}
          >
            {/* Header row */}
            <div />
            {week1.map((d, i) => (
              <div key={i} className={`text-center text-xs pb-2 px-0.5 ${d.dateStr < todayStr ? "opacity-40" : ""}`}>
                <div className="font-medium capitalize">{d.dayLabel}</div>
                <div className="text-muted-foreground">
                  {d.dayNum} {d.monthLabel}
                </div>
              </div>
            ))}
            <div />
            {week2.map((d, i) => (
              <div key={i + 7} className={`text-center text-xs pb-2 px-0.5 ${d.dateStr < todayStr ? "opacity-40" : ""}`}>
                <div className="font-medium capitalize">{d.dayLabel}</div>
                <div className="text-muted-foreground">
                  {d.dayNum} {d.monthLabel}
                </div>
              </div>
            ))}

            {/* Hour rows */}
            {HOURS.map((hour) => (
              <Fragment key={hour}>
                <div className="flex items-center justify-end pr-2 text-xs text-muted-foreground h-11">
                  {String(hour).padStart(2, "0")}:00
                </div>
                {week1.map((d, i) => {
                  const key = `${d.dateStr}:${hour}`;
                  const isOn = selected.has(key);
                  return (
                    <button
                      key={i}
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
                <div className="border-l border-border/30 mx-1" />
                {week2.map((d, i) => {
                  const key = `${d.dateStr}:${hour}`;
                  const isOn = selected.has(key);
                  return (
                    <button
                      key={i + 7}
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
              </Fragment>
            ))}
          </div>
        </div>

        {error && <p className="text-sm text-destructive">{error}</p>}
        {saved && <p className="text-sm text-green-600">Horario guardado.</p>}

        <Button onClick={save} disabled={saving}>
          {saving ? "Guardando…" : "Guardar horario"}
        </Button>
      </CardContent>
    </Card>
  );
}
