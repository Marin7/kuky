import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  createAdminBooking,
  getExtendedClassEligibleStudentIds,
  type AdminBooking,
  type ApiError,
} from "@/lib/admin";
import { useTeacherTimezone } from "@/hooks/useTeacherTimezone";
import { StudentMultiSelect } from "@/components/admin/homework/StudentMultiSelect";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated: (booking: AdminBooking) => void;
}

const ERROR_KEY: Record<string, string> = {
  SLOT_UNAVAILABLE: "slotUnavailableError",
  NOT_A_STUDENT: "notStudentError",
  INVALID_DURATION: "invalidDurationError",
  EXTENDED_CLASS_NOT_ELIGIBLE: "notEligibleForExtendedError",
  USER_NOT_FOUND: "userNotFoundError",
};

const HOURS = Array.from({ length: 12 }, (_, i) =>
  String(i + 8).padStart(2, "0"),
); // 08–19 (8 AM – 7 PM)
const MINUTES = ["00", "15", "30", "45"] as const;

export function CreateBookingDialog({ open, onOpenChange, onCreated }: Props) {
  const { t } = useTranslation();
  const teacherTimezone = useTeacherTimezone();
  const [selected, setSelected] = useState<string[]>([]);
  const [date, setDate] = useState("");
  const [hour, setHour] = useState("");
  const [minute, setMinute] = useState("");
  const [duration, setDuration] = useState<60 | 90>(60);
  const [eligibleIds, setEligibleIds] = useState<Set<string>>(new Set());
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    getExtendedClassEligibleStudentIds()
      .then((ids) => setEligibleIds(new Set(ids)))
      .catch(() => setEligibleIds(new Set()));
  }, [open]);

  const handleChange = (ids: string[]) => {
    const added = ids.find((id) => !selected.includes(id));
    const next = added ? [added] : [];
    setSelected(next);
    if (next.length === 1 && duration === 90 && !eligibleIds.has(next[0])) {
      setDuration(60);
    }
  };

  const time = hour && minute ? `${hour}:${minute}` : "";
  const canSave = selected.length === 1 && date !== "" && time !== "";

  const handleSave = async () => {
    if (!canSave) return;
    setSaving(true);
    setError(null);
    try {
      const created = await createAdminBooking({
        studentId: selected[0],
        date,
        time,
        durationMinutes: duration,
      });
      onCreated(created);
      onOpenChange(false);
      setSelected([]);
      setDate("");
      setHour("");
      setMinute("");
      setDuration(60);
    } catch (e) {
      const err = e as ApiError;
      const key = ERROR_KEY[err.error];
      setError(
        t(`admin.bookings.create.${key ?? "genericError"}` as never) as string,
      );
    } finally {
      setSaving(false);
    }
  };

  const selectedEligible =
    selected.length === 1 && eligibleIds.has(selected[0]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>{t("admin.bookings.create.dialogTitle")}</DialogTitle>
          <DialogDescription>
            {t("admin.bookings.create.dialogDescription", {
              timezone: teacherTimezone,
            })}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-3">
          <div className="space-y-1.5">
            <Label>{t("admin.bookings.create.studentLabel")}</Label>
            <StudentMultiSelect selected={selected} onChange={handleChange} />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="admin-booking-date">
              {t("admin.bookings.create.dateLabel")}
            </Label>
            <Input
              id="admin-booking-date"
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
          </div>

          <div className="space-y-1.5">
            <Label>{t("admin.bookings.create.timeLabel")}</Label>
            <div className="flex items-center gap-2">
              <Select value={hour || undefined} onValueChange={setHour}>
                <SelectTrigger
                  className="w-20"
                  aria-label={t("admin.bookings.create.hourLabel")}
                >
                  <SelectValue
                    placeholder={t("admin.bookings.create.hourPlaceholder")}
                  />
                </SelectTrigger>
                <SelectContent>
                  {HOURS.map((h) => (
                    <SelectItem key={h} value={h}>
                      {h}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <span className="text-muted-foreground" aria-hidden>
                :
              </span>
              <Select value={minute || undefined} onValueChange={setMinute}>
                <SelectTrigger
                  className="w-20"
                  aria-label={t("admin.bookings.create.minuteLabel")}
                >
                  <SelectValue
                    placeholder={t("admin.bookings.create.minutePlaceholder")}
                  />
                </SelectTrigger>
                <SelectContent>
                  {MINUTES.map((m) => (
                    <SelectItem key={m} value={m}>
                      {m}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-1.5">
            <Label>{t("admin.bookings.create.durationLabel")}</Label>
            <div className="flex gap-2">
              <Button
                type="button"
                size="sm"
                variant={duration === 60 ? "default" : "outline"}
                onClick={() => setDuration(60)}
              >
                {t("schedule.duration60")}
              </Button>
              <Button
                type="button"
                size="sm"
                variant={duration === 90 ? "default" : "outline"}
                disabled={selected.length === 1 && !selectedEligible}
                onClick={() => setDuration(90)}
                title={
                  selected.length === 1 && !selectedEligible
                    ? t("admin.bookings.create.notEligibleForExtendedError")
                    : undefined
                }
              >
                {t("schedule.duration90")}
              </Button>
            </div>
          </div>
        </div>

        {error && <p className="text-sm text-destructive">{error}</p>}

        <DialogFooter>
          <Button
            variant="ghost"
            onClick={() => onOpenChange(false)}
            disabled={saving}
          >
            {t("common.cancel")}
          </Button>
          <Button onClick={handleSave} disabled={saving || !canSave}>
            {saving
              ? t("admin.bookings.create.saving")
              : t("admin.bookings.create.save")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
