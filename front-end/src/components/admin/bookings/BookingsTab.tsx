import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getAdminBookings,
  cancelAdminBooking,
  type AdminBooking,
} from "@/lib/admin";
import { useTeacherTimezone } from "@/hooks/useTeacherTimezone";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { StudentLink } from "@/components/admin/students/StudentLink";

function formatSlot(
  isoStart: string,
  isoEnd: string,
  timezone: string,
): string {
  const start = new Date(isoStart);
  const end = new Date(isoEnd);
  const datePart = new Intl.DateTimeFormat("es", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: timezone,
  }).format(start);
  const timePart = new Intl.DateTimeFormat("es", {
    hour: "2-digit",
    minute: "2-digit",
    timeZone: timezone,
  }).format(start);
  const endTime = new Intl.DateTimeFormat("es", {
    hour: "2-digit",
    minute: "2-digit",
    timeZone: timezone,
  }).format(end);
  return `${datePart}, ${timePart}–${endTime}`;
}

export function BookingsTab() {
  const { t } = useTranslation();
  const teacherTimezone = useTeacherTimezone();
  const [bookings, setBookings] = useState<AdminBooking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState<string | null>(null);
  const [cancelError, setCancelError] = useState<string | null>(null);

  useEffect(() => {
    getAdminBookings()
      .then(setBookings)
      .catch(() => setError(t("admin.bookings.loadError")))
      .finally(() => setLoading(false));
  }, []);

  const handleCancel = async (b: AdminBooking) => {
    if (!window.confirm(t("admin.bookings.cancelConfirm"))) return;
    setCancelling(b.id);
    setCancelError(null);
    try {
      await cancelAdminBooking(b.id);
      setBookings((prev) => prev.filter((x) => x.id !== b.id));
    } catch {
      setCancelError(t("admin.bookings.cancelError"));
    } finally {
      setCancelling(null);
    }
  };

  if (loading) {
    return (
      <p className="text-sm text-muted-foreground animate-pulse">
        {t("admin.bookings.loading")}
      </p>
    );
  }

  if (error) {
    return <p className="text-sm text-destructive">{error}</p>;
  }

  if (bookings.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("admin.bookings.empty")}
      </p>
    );
  }

  return (
    <div className="space-y-3">
      <p className="text-sm text-muted-foreground">
        {t("admin.bookings.description")}
      </p>
      {cancelError && <p className="text-sm text-destructive">{cancelError}</p>}
      {bookings.map((b) => (
        <Card key={b.id} className="text-sm">
          <CardContent className="pt-4 space-y-1">
            <p className="font-medium capitalize">
              {formatSlot(b.slotStart, b.slotEnd, teacherTimezone)}
            </p>
            <StudentLink
              student={{
                id: b.studentId,
                email: b.studentEmail,
                firstName: b.studentFirstName,
                lastName: b.studentLastName,
                username: b.studentUsername,
              }}
              showEmail
            />
            {b.zoomJoinUrl && (
              <a
                href={b.zoomJoinUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary text-xs break-all hover:underline"
              >
                {b.zoomJoinUrl}
              </a>
            )}
            <div className="pt-1">
              <Button
                variant="outline"
                size="sm"
                disabled={cancelling === b.id}
                onClick={() => handleCancel(b)}
                className="h-7 text-xs"
              >
                {cancelling === b.id
                  ? t("admin.bookings.cancelling")
                  : t("admin.bookings.cancelClass")}
              </Button>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
