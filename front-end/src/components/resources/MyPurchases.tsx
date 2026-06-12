import { useEffect, useRef, useState } from "react";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import { useTranslation } from "react-i18next";
import {
  listPurchases,
  getReceipt,
  formatEur,
  type PurchaseSummary,
  type ReceiptResponse,
} from "@/lib/resources";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Printer } from "lucide-react";

interface MyPurchasesProps {
  onRefreshRef?: React.MutableRefObject<(() => void) | null>;
  onCatalogRefresh?: () => void;
}

export function MyPurchases({
  onRefreshRef,
  onCatalogRefresh,
}: MyPurchasesProps) {
  const { t } = useTranslation();
  const [purchases, setPurchases] = useState<PurchaseSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [receipt, setReceipt] = useState<ReceiptResponse | null>(null);
  const [receiptLoading, setReceiptLoading] = useState(false);

  const load = () => {
    setLoading(true);
    listPurchases()
      .then((r) => setPurchases(r.purchases))
      .catch(() => setPurchases([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  if (onRefreshRef) {
    onRefreshRef.current = load;
  }

  const handleViewReceipt = async (id: string) => {
    setReceiptLoading(true);
    try {
      const r = await getReceipt(id);
      setReceipt(r);
    } catch {
      // ignore
    } finally {
      setReceiptLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="mx-auto max-w-6xl px-6 pb-10">
        <Skeleton className="h-6 w-40 mb-4" />
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div
              key={i}
              className="flex items-center justify-between rounded-lg border border-border p-4"
            >
              <div className="space-y-2">
                <Skeleton className="h-4 w-48" />
                <Skeleton className="h-3 w-64" />
              </div>
              <Skeleton className="h-8 w-20 rounded-md" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-6 pb-10">
      <h2 className="text-xl font-semibold text-foreground mb-4">
        {t("resources.myPurchases.title")}
      </h2>

      {purchases.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {t("resources.myPurchases.empty")}
        </p>
      ) : (
        <div className="space-y-3">
          {purchases.map((p) => (
            <div
              key={p.id}
              className="flex items-center justify-between rounded-lg border border-border p-4"
            >
              <div>
                <p className="font-medium text-sm">{p.title || p.slug}</p>
                <p className="text-xs text-muted-foreground mt-0.5">
                  {new Intl.DateTimeFormat("es-ES", {
                    dateStyle: "medium",
                  }).format(new Date(p.purchasedAt))}{" "}
                  · {formatEur(p.amountCents)} · {p.receiptReference}
                </p>
              </div>
              <Button
                variant="outline"
                size="sm"
                disabled={receiptLoading}
                onClick={() => handleViewReceipt(p.id)}
              >
                <Printer className="h-3.5 w-3.5 mr-1.5" />
                {t("resources.myPurchases.receiptButton")}
              </Button>
            </div>
          ))}
        </div>
      )}

      {receipt && (
        <ReceiptView receipt={receipt} onClose={() => setReceipt(null)} />
      )}
    </div>
  );
}

interface ReceiptViewProps {
  receipt: ReceiptResponse;
  onClose: () => void;
}

function ReceiptView({ receipt, onClose }: ReceiptViewProps) {
  const { t } = useTranslation();
  return (
    <DialogPrimitive.Root
      open
      onOpenChange={(o) => {
        if (!o) onClose();
      }}
    >
      <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-black/50 print:hidden" />
      <DialogPrimitive.Content
        aria-describedby={undefined}
        id="receipt-content"
        className="fixed left-1/2 top-1/2 z-50 w-full max-w-sm -translate-x-1/2 -translate-y-1/2 bg-white rounded-lg p-8 shadow-xl focus:outline-none print:static print:translate-x-0 print:translate-y-0 print:shadow-none print:rounded-none"
      >
        <div className="text-center mb-6">
          <DialogPrimitive.Title className="text-lg font-bold">
            {t("resources.myPurchases.brand")}
          </DialogPrimitive.Title>
          <p className="text-xs text-gray-500">
            {t("resources.myPurchases.receiptTitle")}
          </p>
        </div>

        <div className="space-y-2 text-sm mb-6">
          <div className="flex justify-between">
            <span className="text-gray-500">
              {t("resources.myPurchases.reference")}
            </span>
            <span className="font-medium">{receipt.receiptReference}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">
              {t("resources.myPurchases.date")}
            </span>
            <span>
              {new Intl.DateTimeFormat("es-ES", { dateStyle: "long" }).format(
                new Date(receipt.purchasedAt),
              )}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">
              {t("resources.myPurchases.client")}
            </span>
            <span>{receipt.buyerEmail}</span>
          </div>
        </div>

        <div className="border-t border-gray-200 pt-4 mb-4">
          <p className="text-sm font-medium mb-2">{receipt.itemTitle}</p>
          {receipt.lineItems.map((item, i) => (
            <p key={i} className="text-xs text-gray-600 pl-2">
              · {item.title}
            </p>
          ))}
        </div>

        <div className="flex justify-between font-semibold border-t border-gray-200 pt-3">
          <span>{t("resources.myPurchases.total")}</span>
          <span>{formatEur(receipt.amountCents)}</span>
        </div>

        <div className="flex gap-2 mt-6 print:hidden">
          <Button className="flex-1" onClick={() => window.print()}>
            <Printer className="h-4 w-4 mr-2" />
            {t("resources.myPurchases.print")}
          </Button>
          <Button variant="outline" onClick={onClose}>
            {t("resources.myPurchases.close")}
          </Button>
        </div>
      </DialogPrimitive.Content>
    </DialogPrimitive.Root>
  );
}
