import { useEffect, useState } from "react";
import {
  listBookings,
  cancelBooking,
  type BookingSummary,
  type MyBookingsResponse,
  type ApiError,
} from "@/lib/scheduling";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

function formatSlot(iso: string): string {
  return new Intl.DateTimeFormat("es", {
    weekday: "long",
    day: "numeric",
    month: "long",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(iso));
}

interface BookingCardProps {
  booking: BookingSummary;
  onCancel: (id: string) => void;
  cancelling: string | null;
}

function BookingCard({ booking, onCancel, cancelling }: BookingCardProps) {
  return (
    <Card className="text-sm">
      <CardContent className="pt-4 space-y-2">
        <p className="font-medium">{formatSlot(booking.slotStart)}</p>
        {booking.zoomJoinUrl && (
          <a
            href={booking.zoomJoinUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-primary text-xs break-all hover:underline"
          >
            {booking.zoomJoinUrl}
          </a>
        )}
        <div className="flex items-center gap-2">
          <span
            className={[
              "inline-block rounded-full px-2 py-0.5 text-xs font-medium",
              booking.status === "CONFIRMED"
                ? "bg-green-100 text-green-700"
                : "bg-muted text-muted-foreground",
            ].join(" ")}
          >
            {booking.status === "CONFIRMED" ? "Confirmada" : "Cancelada"}
          </span>
          {booking.cancellable && (
            <Button
              variant="outline"
              size="sm"
              disabled={cancelling === booking.id}
              onClick={() => onCancel(booking.id)}
              className="h-7 text-xs"
            >
              {cancelling === booking.id ? "Cancelando…" : "Cancelar clase"}
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

interface MyBookingsProps {
  onScheduleRefresh?: () => void;
  onRefreshRef?: React.MutableRefObject<(() => void) | null>;
}

export function MyBookings({
  onScheduleRefresh,
  onRefreshRef,
}: MyBookingsProps) {
  const [data, setData] = useState<MyBookingsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState<string | null>(null);
  const [cancelError, setCancelError] = useState<string | null>(null);

  const fetch = () => {
    setLoading(true);
    listBookings()
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetch();
    if (onRefreshRef) onRefreshRef.current = fetch;
  }, []);

  const handleCancel = async (id: string) => {
    setCancelling(id);
    setCancelError(null);
    try {
      await cancelBooking(id);
      fetch();
      onScheduleRefresh?.();
    } catch (e) {
      const err = e as ApiError;
      if (err.error === "CANCELLATION_TOO_LATE") {
        setCancelError(
          "No puedes cancelar con menos de 24 horas de antelación.",
        );
      } else {
        setCancelError("No se pudo cancelar la clase. Inténtalo de nuevo.");
      }
    } finally {
      setCancelling(null);
    }
  };

  // Secondary panel: render nothing while loading and when there are no
  // bookings, so users without classes never see it appear then vanish.
  if (loading) return null;
  if (!data || (data.upcoming.length === 0 && data.past.length === 0))
    return null;

  return (
    <div className="mx-auto max-w-5xl px-4 pb-10 space-y-6">
      <h2 className="font-display text-xl font-bold">Mis clases</h2>

      {cancelError && <p className="text-sm text-destructive">{cancelError}</p>}

      {data.upcoming.length > 0 && (
        <section className="space-y-3">
          <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
            Próximas
          </h3>
          {data.upcoming.map((b) => (
            <BookingCard
              key={b.id}
              booking={b}
              onCancel={handleCancel}
              cancelling={cancelling}
            />
          ))}
        </section>
      )}

      {data.past.length > 0 && (
        <section className="space-y-3">
          <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
            Pasadas
          </h3>
          {data.past.map((b) => (
            <BookingCard
              key={b.id}
              booking={b}
              onCancel={handleCancel}
              cancelling={cancelling}
            />
          ))}
        </section>
      )}
    </div>
  );
}
