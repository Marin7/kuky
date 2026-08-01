import { Fragment, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getAvailability,
  updateWeekly,
  type WeeklyWindow,
  type BookingConflict,
  type ApiError,
} from "@/lib/admin";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

// Debounce autosave so rapid clicks across several cells only trigger one PUT request.
const AUTOSAVE_DEBOUNCE_MS = 600;

const DAY_ABBREVS = ["", "lun", "mar", "mié", "jue", "vie", "sáb", "dom"];
const DOWS = [1, 2, 3, 4, 5, 6, 7];

const HOUR_START = 8;
const HOUR_END = 20;
const HOURS = Array.from(
  { length: HOUR_END - HOUR_START },
  (_, i) => HOUR_START + i,
);

function computeSelected(weekly: WeeklyWindow[]): Set<string> {
  const selected = new Set<string>();
  for (const w of weekly) {
    const s = parseInt(w.startTime.split(":")[0]);
    const e = parseInt(w.endTime.split(":")[0]);
    for (let hour = s; hour < e; hour++) {
      if (hour >= HOUR_START && hour < HOUR_END) {
        selected.add(`${w.dayOfWeek}:${hour}`);
      }
    }
  }
  return selected;
}

function selectedToWindows(selected: Set<string>): WeeklyWindow[] {
  const windows: WeeklyWindow[] = [];
  for (const dow of DOWS) {
    const hours = HOURS.filter((h) => selected.has(`${dow}:${h}`)).sort(
      (a, b) => a - b,
    );
    let start: number | null = null;
    let prev: number | null = null;
    for (let i = 0; i <= hours.length; i++) {
      const cur = hours[i];
      if (start === null) {
        start = cur;
        prev = cur;
      } else if (cur !== undefined && prev !== null && cur === prev + 1) {
        prev = cur;
      } else {
        windows.push({
          dayOfWeek: dow,
          startTime: `${String(start).padStart(2, "0")}:00`,
          endTime: `${String((prev as number) + 1).padStart(2, "0")}:00`,
        });
        start = cur ?? null;
        prev = cur ?? null;
      }
    }
  }
  return windows;
}

interface Props {
  onConflicts: (conflicts: BookingConflict[]) => void;
}

export function GeneralAvailabilityEditor({ onConflicts }: Props) {
  const { t } = useTranslation();
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  // Mirrors `selected` synchronously so the debounced save always reads the latest value,
  // instead of a value captured by a stale closure from an earlier render.
  const latestSelectedRef = useRef<Set<string>>(new Set());
  const saveTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const applySelected = (s: Set<string>) => {
    setSelected(s);
    latestSelectedRef.current = s;
  };

  useEffect(() => {
    getAvailability()
      .then(({ weekly }) => applySelected(computeSelected(weekly)))
      .catch(() => setError(t("admin.availability.loadError")))
      .finally(() => setLoading(false));
  }, []);

  // Cancel any pending debounced save on unmount so it doesn't fire against a gone component.
  useEffect(() => {
    return () => {
      if (saveTimer.current) clearTimeout(saveTimer.current);
    };
  }, []);

  const scheduleSave = () => {
    if (saveTimer.current) clearTimeout(saveTimer.current);
    saveTimer.current = setTimeout(async () => {
      saveTimer.current = null;
      setSaved(false);
      setError(null);
      setSaving(true);
      try {
        const res = await updateWeekly(
          selectedToWindows(latestSelectedRef.current),
        );
        applySelected(computeSelected(res.weekly));
        onConflicts(res.bookingConflicts);
        setSaved(true);
      } catch (e) {
        setError((e as ApiError).message ?? t("admin.availability.saveError"));
        // Resync with the server so the grid doesn't keep showing a change that failed to persist.
        getAvailability()
          .then(({ weekly }) => applySelected(computeSelected(weekly)))
          .catch(() => {});
      } finally {
        setSaving(false);
      }
    }, AUTOSAVE_DEBOUNCE_MS);
  };

  const toggle = (dow: number, hour: number) => {
    const key = `${dow}:${hour}`;
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      latestSelectedRef.current = next;
      return next;
    });
    setSaved(false);
    scheduleSave();
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
          {t("admin.availability.general.title")}
        </CardTitle>
        <p className="text-sm text-muted-foreground">
          {t("admin.availability.general.description")}
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        <div
          className="grid w-full gap-x-1.5 gap-y-1"
          style={{
            gridTemplateColumns: `5.5rem repeat(7, minmax(0, 1fr))`,
          }}
        >
          <div />
          {DOWS.map((dow) => (
            <div
              key={dow}
              className="text-center text-sm pb-2 font-medium capitalize"
            >
              {DAY_ABBREVS[dow]}
            </div>
          ))}

          {HOURS.map((hour) => (
            <Fragment key={hour}>
              <div className="flex items-center justify-end pr-2 text-xs text-muted-foreground h-12 whitespace-nowrap">
                {String(hour).padStart(2, "0")}:00-
                {String(hour + 1).padStart(2, "0")}:00
              </div>
              {DOWS.map((dow) => {
                const isOn = selected.has(`${dow}:${hour}`);
                return (
                  <button
                    key={dow}
                    onClick={() => toggle(dow, hour)}
                    className={`h-12 rounded border transition-colors ${
                      isOn
                        ? "bg-primary/15 border-primary/60 hover:bg-primary/25"
                        : "border-border/40 hover:bg-muted/50"
                    }`}
                  />
                );
              })}
            </Fragment>
          ))}
        </div>

        {error && <p className="text-sm text-destructive">{error}</p>}
        {saving ? (
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
