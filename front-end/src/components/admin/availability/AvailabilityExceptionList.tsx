import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getAvailability,
  addException,
  deleteException,
  type AvailabilityException,
  type ApiError,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat("es", {
    weekday: "long",
    day: "numeric",
    month: "long",
  }).format(new Date(`${iso}T00:00:00`));
}

export function AvailabilityExceptionList() {
  const { t } = useTranslation();
  const [exceptions, setExceptions] = useState<AvailabilityException[]>([]);
  const [loading, setLoading] = useState(true);
  const [date, setDate] = useState("");
  const [kind, setKind] = useState<"BLOCK" | "OPEN">("BLOCK");
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("18:00");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    getAvailability()
      .then((a) => setExceptions(a.exceptions))
      .catch(() => setError(t("admin.availability.exceptions.loadError")))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const add = async () => {
    if (!date) {
      setError(t("admin.availability.exceptions.noDate"));
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await addException(date, kind, startTime, endTime);
      setDate("");
      load();
    } catch (e) {
      setError(
        (e as ApiError).message ?? t("admin.availability.exceptions.addError"),
      );
    } finally {
      setSaving(false);
    }
  };

  const remove = async (id: string) => {
    await deleteException(id);
    setExceptions((prev) => prev.filter((e) => e.id !== id));
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">
          {t("admin.availability.exceptions.title")}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap items-end gap-2">
          <Input
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="h-8 w-40"
          />
          <Select
            value={kind}
            onValueChange={(v) => setKind(v as "BLOCK" | "OPEN")}
          >
            <SelectTrigger className="h-8 w-32">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="BLOCK">
                {t("admin.availability.exceptions.block")}
              </SelectItem>
              <SelectItem value="OPEN">
                {t("admin.availability.exceptions.open")}
              </SelectItem>
            </SelectContent>
          </Select>
          <Input
            type="time"
            value={startTime}
            onChange={(e) => setStartTime(e.target.value)}
            className="h-8 w-28"
          />
          <span className="text-muted-foreground">–</span>
          <Input
            type="time"
            value={endTime}
            onChange={(e) => setEndTime(e.target.value)}
            className="h-8 w-28"
          />
          <Button size="sm" onClick={add} disabled={saving} className="h-8">
            {saving
              ? t("admin.availability.exceptions.adding")
              : t("admin.availability.exceptions.add")}
          </Button>
        </div>

        {error && <p className="text-sm text-destructive">{error}</p>}

        {loading ? (
          <p className="text-sm text-muted-foreground">
            {t("admin.availability.exceptions.loading")}
          </p>
        ) : exceptions.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            {t("admin.availability.exceptions.noExceptions")}
          </p>
        ) : (
          <ul className="space-y-2">
            {exceptions.map((e) => (
              <li
                key={e.id}
                className="flex items-center justify-between text-sm"
              >
                <span>
                  <span
                    className={[
                      "mr-2 inline-block rounded-full px-2 py-0.5 text-xs font-medium",
                      e.kind === "BLOCK"
                        ? "bg-red-100 text-red-700"
                        : "bg-green-100 text-green-700",
                    ].join(" ")}
                  >
                    {e.kind === "BLOCK"
                      ? t("admin.availability.exceptions.blocked")
                      : t("admin.availability.exceptions.opened")}
                  </span>
                  {formatDate(e.date)} · {e.startTime}–{e.endTime}
                </span>
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-7 text-xs text-destructive"
                  onClick={() => remove(e.id)}
                >
                  {t("admin.availability.exceptions.remove")}
                </Button>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}
