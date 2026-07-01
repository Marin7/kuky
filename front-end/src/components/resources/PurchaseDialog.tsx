import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import {
  formatEur,
  purchase,
  type ApiError,
  type ItemType,
} from "@/lib/resources";
import { useNavigate } from "@tanstack/react-router";
import { StudentOnlyNotice } from "@/components/StudentOnlyNotice";

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

export function PurchaseDialog({
  target,
  onClose,
  onSuccess,
}: PurchaseDialogProps) {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmed, setConfirmed] = useState(false);
  const [studentOnly, setStudentOnly] = useState(false);
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
        setError(t("resources.purchase.alreadyOwnedError"));
      } else if (apiErr.error === "NOT_PURCHASABLE") {
        setError(t("resources.purchase.notPurchasableError"));
      } else if (apiErr.error === "ACCESS_DENIED") {
        setStudentOnly(true);
      } else {
        navigate({
          to: "/cuenta",
          search: {
            returnTo: "/recursos",
            buy: `${target.itemType}:${target.slug}`,
          } as Record<string, string>,
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
      setStudentOnly(false);
      onClose();
    }
  };

  return (
    <Dialog open={!!target} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>
            {confirmed
              ? t("resources.purchase.confirmedTitle")
              : t("resources.purchase.confirmTitle")}
          </DialogTitle>
        </DialogHeader>

        {studentOnly ? (
          <div className="py-4">
            <StudentOnlyNotice />
          </div>
        ) : confirmed ? (
          <div className="py-4 space-y-3">
            <p className="text-sm text-muted-foreground">
              {t("resources.purchase.confirmedTitle")}{" "}
              <strong>{target?.title}</strong>.
            </p>
            <DialogFooter>
              <Button onClick={onClose}>{t("resources.purchase.close")}</Button>
            </DialogFooter>
          </div>
        ) : (
          <div className="py-4 space-y-4">
            <div className="rounded-md bg-amber-50 border border-amber-200 p-3 text-sm text-amber-800">
              ⚠️ {t("resources.purchase.demoWarning")}
            </div>

            {target && (
              <div className="space-y-1">
                <p className="font-medium">{target.title}</p>
                <p className="text-lg font-semibold">
                  {formatEur(target.priceCents)}
                </p>
              </div>
            )}

            {error && <p className="text-sm text-destructive">{error}</p>}

            <DialogFooter className="gap-2">
              <Button variant="outline" onClick={onClose} disabled={loading}>
                {t("resources.purchase.cancel")}
              </Button>
              <Button onClick={handleConfirm} disabled={loading}>
                {loading
                  ? t("resources.purchase.processing")
                  : t("resources.purchase.confirmButton")}
              </Button>
            </DialogFooter>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
