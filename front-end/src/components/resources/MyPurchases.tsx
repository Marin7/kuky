import { useEffect, useRef, useState } from "react";
import { listPurchases, getReceipt, formatEur, type PurchaseSummary, type ReceiptResponse, type ApiError } from "@/lib/resources";
import { Button } from "@/components/ui/button";
import { Printer } from "lucide-react";

interface MyPurchasesProps {
  onRefreshRef?: React.MutableRefObject<(() => void) | null>;
  onCatalogRefresh?: () => void;
}

export function MyPurchases({ onRefreshRef, onCatalogRefresh }: MyPurchasesProps) {
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
        <p className="text-muted-foreground animate-pulse">Cargando tus recursos…</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-6 pb-10">
      <h2 className="text-xl font-semibold text-foreground mb-4">Mis recursos</h2>

      {purchases.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          Aún no has adquirido ningún recurso.
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
                Recibo
              </Button>
            </div>
          ))}
        </div>
      )}

      {receipt && <ReceiptView receipt={receipt} onClose={() => setReceipt(null)} />}
    </div>
  );
}

interface ReceiptViewProps {
  receipt: ReceiptResponse;
  onClose: () => void;
}

function ReceiptView({ receipt, onClose }: ReceiptViewProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 print:bg-white print:inset-auto print:relative print:flex-none">
      <div className="bg-white rounded-lg p-8 max-w-sm w-full shadow-xl print:shadow-none print:rounded-none" id="receipt-content">
        <div className="text-center mb-6">
          <h3 className="text-lg font-bold">Español con Paula</h3>
          <p className="text-xs text-gray-500">Recibo de compra</p>
        </div>

        <div className="space-y-2 text-sm mb-6">
          <div className="flex justify-between">
            <span className="text-gray-500">Referencia</span>
            <span className="font-medium">{receipt.receiptReference}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Fecha</span>
            <span>{new Intl.DateTimeFormat("es-ES", { dateStyle: "long" }).format(new Date(receipt.purchasedAt))}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Cliente</span>
            <span>{receipt.buyerEmail}</span>
          </div>
        </div>

        <div className="border-t border-gray-200 pt-4 mb-4">
          <p className="text-sm font-medium mb-2">{receipt.itemTitle}</p>
          {receipt.lineItems.map((item, i) => (
            <p key={i} className="text-xs text-gray-600 pl-2">· {item.title}</p>
          ))}
        </div>

        <div className="flex justify-between font-semibold border-t border-gray-200 pt-3">
          <span>Total</span>
          <span>{formatEur(receipt.amountCents)}</span>
        </div>

        <div className="flex gap-2 mt-6 print:hidden">
          <Button className="flex-1" onClick={() => window.print()}>
            <Printer className="h-4 w-4 mr-2" />
            Imprimir / PDF
          </Button>
          <Button variant="outline" onClick={onClose}>
            Cerrar
          </Button>
        </div>
      </div>
    </div>
  );
}
