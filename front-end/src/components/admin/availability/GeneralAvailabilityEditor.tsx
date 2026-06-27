import { Fragment, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getAvailability,
  updateWeekly,
  type WeeklyWindow,
  type BookingConflict,
  type ApiError,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const DAY_ABBREVS = ["", "lun", "mar", "mié", "jue", "vie", "sáb", "dom"];
const DOWS = [1, 2, 3, 4, 5, 6, 7];

const HOUR_START = 7;
const HOUR_END = 21;
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

  useEffect(() => {
    getAvailability()
      .then(({ weekly }) => setSelected(computeSelected(weekly)))
      .catch(() => setError(t("admin.availability.loadError")))
      .finally(() => setLoading(false));
  }, []);

  const toggle = (dow: number, hour: number) => {
    const key = `${dow}:${hour}`;
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
      const res = await updateWeekly(selectedToWindows(selected));
      setSelected(computeSelected(res.weekly));
      onConflicts(res.bookingConflicts);
      setSaved(true);
    } catch (e) {
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
          {t("admin.availability.general.title")}
        </CardTitle>
        <p className="text-sm text-muted-foreground">
          {t("admin.availability.general.description")}
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="overflow-x-auto">
          <div
            className="inline-grid gap-y-1"
            style={{
              gridTemplateColumns: `3rem repeat(7, minmax(44px, 1fr))`,
            }}
          >
            <div />
            {DOWS.map((dow) => (
              <div
                key={dow}
                className="text-center text-xs pb-2 px-0.5 font-medium capitalize"
              >
                {DAY_ABBREVS[dow]}
              </div>
            ))}

            {HOURS.map((hour) => (
              <Fragment key={hour}>
                <div className="flex items-center justify-end pr-2 text-xs text-muted-foreground h-11">
                  {String(hour).padStart(2, "0")}:00
                </div>
                {DOWS.map((dow) => {
                  const isOn = selected.has(`${dow}:${hour}`);
                  return (
                    <button
                      key={dow}
                      onClick={() => toggle(dow, hour)}
                      className={`h-11 mx-0.5 rounded border transition-colors ${
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
