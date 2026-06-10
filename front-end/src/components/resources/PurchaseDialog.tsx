import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { formatEur, purchase, type ApiError, type ItemType } from "@/lib/resources";
import { useNavigate } from "@tanstack/react-router";

interface PurchaseTarget {
  itemType: ItemType;
  slug: string;
  title: string;
  priceCents: number;
}

interface PurchaseDialogProps {
  target: PurchaseTarget | null;
  onClose: () => void;
  onSuccess: () => void;
}

export function PurchaseDialog({ target, onClose, onSuccess }: PurchaseDialogProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmed, setConfirmed] = useState(false);
  const navigate = useNavigate();

  const handleConfirm = async () => {
    if (!target) return;
    setLoading(true);
    setError(null);
    try {
      await purchase(target.itemType, target.slug);
      setConfirmed(true);
      onSuccess();
    } catch (e) {
      const apiErr = e as ApiError;
      if (apiErr.error === "ALREADY_OWNED") {
        setError("Ya tienes acceso a este recurso.");
      } else if (apiErr.error === "NOT_PURCHASABLE") {
        setError("Este recurso es gratuito.");
      } else {
        // 401 — redirect to sign in with return intent encoded in search params
        // FR-009: return to purchase after sign in
        navigate({
          to: "/cuenta",
          search: { returnTo: "/recursos", buy: `${target.itemType}:${target.slug}` } as Record<string, string>,
        });
        onClose();
        return;
      }
    } finally {
      setLoading(false);
    }
  };

  const handleOpenChange = (open: boolean) => {
    if (!open) {
      setError(null);
      setConfirmed(false);
      onClose();
    }
  };

  return (
    <Dialog open={!!target} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>
            {confirmed ? "¡Recurso desbloqueado!" : "Confirmar compra"}
          </DialogTitle>
        </DialogHeader>

        {confirmed ? (
          <div className="py-4 space-y-3">
            <p className="text-sm text-muted-foreground">
              Ahora tienes acceso a <strong>{target?.title}</strong>. Puedes
              ver los materiales directamente desde el catálogo.
            </p>
            <DialogFooter>
              <Button onClick={onClose}>Cerrar</Button>
            </DialogFooter>
          </div>
        ) : (
          <div className="py-4 space-y-4">
            <div className="rounded-md bg-amber-50 border border-amber-200 p-3 text-sm text-amber-800">
              ⚠️ Esta es una versión de demostración — no se realiza ningún pago real.
              El acceso se concede automáticamente al confirmar.
            </div>

            {target && (
              <div className="space-y-1">
                <p className="font-medium">{target.title}</p>
                <p className="text-lg font-semibold">{formatEur(target.priceCents)}</p>
              </div>
            )}

            {error && <p className="text-sm text-destructive">{error}</p>}

            <DialogFooter className="gap-2">
              <Button variant="outline" onClick={onClose} disabled={loading}>
                Cancelar
              </Button>
              <Button onClick={handleConfirm} disabled={loading}>
                {loading ? "Procesando…" : "Confirmar y desbloquear"}
              </Button>
            </DialogFooter>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
