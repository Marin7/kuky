import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  attachCompanionStudent,
  type AdminBooking,
  type ApiError,
} from "@/lib/admin";
import { StudentMultiSelect } from "@/components/admin/homework/StudentMultiSelect";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  booking: AdminBooking;
  onAttached: (updated: AdminBooking) => void;
}

const ERROR_KEY: Record<string, string> = {
  COMPANION_ALREADY_ATTACHED: "alreadyAttachedError",
  COMPANION_SAME_AS_BOOKING_STUDENT: "sameAsBookingStudentError",
  COMPANION_NOT_STUDENT: "notStudentError",
  EXTENDED_CLASS_NOT_ELIGIBLE: "notEligibleForExtendedError",
  BOOKING_NOT_ATTACHABLE: "notAttachableError",
};

export function AttachCompanionStudentDialog({
  open,
  onOpenChange,
  booking,
  onAttached,
}: Props) {
  const { t } = useTranslation();
  const [selected, setSelected] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Capped to a single selection — a class supports at most one companion student.
  const handleChange = (ids: string[]) => {
    const added = ids.find((id) => !selected.includes(id));
    setSelected(added ? [added] : []);
  };

  const handleSave = async () => {
    if (selected.length === 0) return;
    setSaving(true);
    setError(null);
    try {
      const updated = await attachCompanionStudent(booking.id, selected[0]);
      onAttached(updated);
      onOpenChange(false);
      setSelected([]);
    } catch (e) {
      const err = e as ApiError;
      const key = ERROR_KEY[err.error];
      setError(
        t(
          `admin.bookings.companion.${key ?? "genericError"}` as never,
        ) as string,
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>{t("admin.bookings.companion.dialogTitle")}</DialogTitle>
        </DialogHeader>

        <StudentMultiSelect selected={selected} onChange={handleChange} />

        {error && <p className="text-sm text-destructive">{error}</p>}

        <DialogFooter>
          <Button
            variant="ghost"
            onClick={() => onOpenChange(false)}
            disabled={saving}
          >
            {t("common.cancel")}
          </Button>
          <Button
            onClick={handleSave}
            disabled={saving || selected.length === 0}
          >
            {saving
              ? t("admin.bookings.companion.saving")
              : t("admin.bookings.companion.save")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
