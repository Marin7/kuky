import { useEffect, useState } from "react";
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

const ERROR_MESSAGES: Record<string, string> = {
  SLOT_UNAVAILABLE: "Esta hora ya no está disponible. Por favor, elige otra.",
  BOOKING_TOO_SOON:
    "Esta hora está demasiado próxima. Reserva con al menos 24 horas de antelación.",
  SLOT_OUT_OF_RANGE: "Esta hora no está disponible para reservar.",
  MEETING_PROVISIONING_FAILED:
    "No se pudo crear la videollamada. Por favor, inténtalo de nuevo.",
};

export function BookingDialog({
  slot,
  isAuthenticated,
  onClose,
  onSuccess,
}: BookingDialogProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [joinUrl, setJoinUrl] = useState<string | null>(null);

  // Reset dialog state each time a different slot is selected
  useEffect(() => {
    if (slot) {
      setJoinUrl(null);
      setError(null);
      setLoading(false);
    }
  }, [slot?.start]);

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
      setError(
        ERROR_MESSAGES[apiErr.error] ??
          "Ha ocurrido un error. Por favor, inténtalo de nuevo.",
      );
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
            {joinUrl ? "¡Reserva confirmada!" : "Confirmar reserva"}
          </DialogTitle>
        </DialogHeader>

        {!isAuthenticated ? (
          <div className="py-4 space-y-3 text-center">
            <p className="text-sm text-muted-foreground">
              Debes iniciar sesión para reservar una clase.
            </p>
            <Button asChild onClick={onClose}>
              <Link to="/cuenta">Iniciar sesión</Link>
            </Button>
          </div>
        ) : joinUrl ? (
          <div className="py-4 space-y-4">
            <p className="text-sm text-muted-foreground">
              Tu clase ha sido reservada para el{" "}
              <strong>{slot ? formatSlotDateTime(slot.start) : ""}</strong>.
            </p>
            <div className="rounded-md bg-muted p-3">
              <p className="text-xs text-muted-foreground mb-1">
                Enlace de Zoom
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
              Recibirás este enlace también por correo electrónico.
            </p>
            <DialogFooter>
              <Button onClick={onClose}>Cerrar</Button>
            </DialogFooter>
          </div>
        ) : (
          <div className="py-4 space-y-4">
            {slot && (
              <p className="text-sm text-muted-foreground">
                Reservar clase para el{" "}
                <strong>{formatSlotDateTime(slot.start)}</strong>.
              </p>
            )}
            {error && <p className="text-sm text-destructive">{error}</p>}
            <DialogFooter className="gap-2">
              <Button variant="outline" onClick={onClose} disabled={loading}>
                Cancelar
              </Button>
              <Button onClick={handleConfirm} disabled={loading}>
                {loading ? "Reservando…" : "Confirmar reserva"}
              </Button>
            </DialogFooter>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
