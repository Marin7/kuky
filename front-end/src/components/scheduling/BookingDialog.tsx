import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { createBooking, type Slot, type ApiError } from "@/lib/scheduling";
import { Link } from "@tanstack/react-router";

interface BookingDialogProps {
  slot: Slot | null;
  isAuthenticated: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

function formatSlotDateTime(iso: string): string {
  return new Intl.DateTimeFormat("es", {
    weekday: "long",
    day: "numeric",
    month: "long",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(iso));
}

export function BookingDialog({
  slot,
  isAuthenticated,
  onClose,
  onSuccess,
}: BookingDialogProps) {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [joinUrl, setJoinUrl] = useState<string | null>(null);

  useEffect(() => {
    if (slot) {
      setJoinUrl(null);
      setError(null);
      setLoading(false);
    }
  }, [slot?.start]);

  const getErrorMessage = (errorCode: string): string => {
    const map: Record<string, string> = {
      SLOT_UNAVAILABLE: t("schedule.booking.slotUnavailableError"),
      BOOKING_TOO_SOON: t("schedule.booking.bookingTooSoonError"),
      SLOT_OUT_OF_RANGE: t("schedule.booking.slotOutOfRangeError"),
      MEETING_PROVISIONING_FAILED: t("schedule.booking.meetingError"),
    };
    return map[errorCode] ?? t("schedule.booking.genericError");
  };

  const handleConfirm = async () => {
    if (!slot) return;
    setLoading(true);
    setError(null);
    try {
      const booking = await createBooking(slot.start);
      setJoinUrl(booking.zoomJoinUrl);
      onSuccess();
    } catch (e) {
      const apiErr = e as ApiError;
      setError(getErrorMessage(apiErr.error));
    } finally {
      setLoading(false);
    }
  };

  const handleOpenChange = (open: boolean) => {
    if (!open) {
      setError(null);
      setJoinUrl(null);
      onClose();
    }
  };

  return (
    <Dialog open={!!slot} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>
            {joinUrl
              ? t("schedule.booking.confirmedTitle")
              : t("schedule.booking.confirmTitle")}
          </DialogTitle>
        </DialogHeader>

        {!isAuthenticated ? (
          <div className="py-4 space-y-3 text-center">
            <p className="text-sm text-muted-foreground">
              {t("schedule.booking.loginRequired")}
            </p>
            <Button asChild onClick={onClose}>
              <Link to="/cuenta">{t("schedule.booking.loginButton")}</Link>
            </Button>
          </div>
        ) : joinUrl ? (
          <div className="py-4 space-y-4">
            <p className="text-sm text-muted-foreground">
              {t("schedule.booking.confirmedTitle")}{" "}
              <strong>{slot ? formatSlotDateTime(slot.start) : ""}</strong>.
            </p>
            <div className="rounded-md bg-muted p-3">
              <p className="text-xs text-muted-foreground mb-1">
                {t("schedule.booking.zoomLink")}
              </p>
              <a
                href={joinUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="text-sm text-primary break-all hover:underline"
              >
                {joinUrl}
              </a>
            </div>
            <p className="text-xs text-muted-foreground">
              {t("schedule.booking.zoomEmailNote")}
            </p>
            <DialogFooter>
              <Button onClick={onClose}>{t("schedule.booking.close")}</Button>
            </DialogFooter>
          </div>
        ) : (
          <div className="py-4 space-y-4">
            {slot && (
              <p className="text-sm text-muted-foreground">
                {t("schedule.booking.confirmTitle")}{" "}
                <strong>{formatSlotDateTime(slot.start)}</strong>.
              </p>
            )}
            {error && <p className="text-sm text-destructive">{error}</p>}
            <DialogFooter className="gap-2">
              <Button variant="outline" onClick={onClose} disabled={loading}>
                {t("schedule.booking.cancel")}
              </Button>
              <Button onClick={handleConfirm} disabled={loading}>
                {loading
                  ? t("schedule.booking.confirming")
                  : t("schedule.booking.confirm")}
              </Button>
            </DialogFooter>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
