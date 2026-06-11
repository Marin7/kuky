import { useEffect, useRef, useState } from "react";
import {
  getCatalog,
  type CatalogResponse,
  type ResourceCard,
  type BundleCard,
} from "@/lib/resources";
import { Skeleton } from "@/components/ui/skeleton";
import { FreeResourcesSection } from "./FreeResourcesSection";
import { ResourceCard as ResourceCardComponent } from "./ResourceCard";
import { BundleCard as BundleCardComponent } from "./BundleCard";
import { ResourceDetailDialog } from "./ResourceDetailDialog";
import { PurchaseDialog } from "./PurchaseDialog";

// Section heading + a grid of card placeholders matching the loaded catalog.
function CatalogSkeleton() {
  return (
    <div className="space-y-4">
      <Skeleton className="h-6 w-44" />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <div
            key={i}
            className="rounded-xl border border-border p-4 space-y-3"
          >
            <Skeleton className="h-32 w-full rounded-lg" />
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-3 w-full" />
            <Skeleton className="h-8 w-24 rounded-md" />
          </div>
        ))}
      </div>
    </div>
  );
}

interface ResourcesViewProps {
  onRefreshRef?: React.MutableRefObject<(() => void) | null>;
  onPurchaseSuccess?: () => void;
}

export function ResourcesView({
  onRefreshRef,
  onPurchaseSuccess,
}: ResourcesViewProps) {
  const [catalog, setCatalog] = useState<CatalogResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [detailResource, setDetailResource] = useState<ResourceCard | null>(
    null,
  );
  const [purchaseTarget, setPurchaseTarget] = useState<{
    itemType: "RESOURCE" | "BUNDLE";
    slug: string;
    title: string;
    priceCents: number;
  } | null>(null);

  const loadCatalog = () => {
    setLoading(true);
    setError(null);
    getCatalog()
      .then(setCatalog)
      .catch(() => setError("No se pudo cargar el catálogo."))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadCatalog();
  }, []);

  if (onRefreshRef) {
    onRefreshRef.current = loadCatalog;
  }

  const handleBuyResource = (slug: string) => {
    // Slug may be a paid resource slug or a free resource slug with a related paid resource
    let targetSlug = slug;
    let resource = catalog?.paidResources.find((r) => r.slug === slug) ?? null;

    if (!resource) {
      // Could be a free resource — redirect to its related paid resource
      const free = catalog?.freeResources.find((r) => r.slug === slug);
      if (free?.relatedResourceSlug) {
        targetSlug = free.relatedResourceSlug;
        resource =
          catalog?.paidResources.find((r) => r.slug === targetSlug) ?? null;
      }
    }

    if (
      resource &&
      resource.pricing === "PAID" &&
      resource.priceCents != null
    ) {
      setPurchaseTarget({
        itemType: "RESOURCE",
        slug: targetSlug,
        title: resource.title,
        priceCents: resource.priceCents,
      });
    }
  };

  const handleBuyBundle = (bundle: BundleCard) => {
    setPurchaseTarget({
      itemType: "BUNDLE",
      slug: bundle.slug,
      title: bundle.title,
      priceCents: bundle.priceCents,
    });
  };

  const handlePurchaseSuccess = () => {
    setPurchaseTarget(null);
    loadCatalog();
    onPurchaseSuccess?.();
  };

  const isEmpty =
    catalog &&
    catalog.freeResources.length === 0 &&
    catalog.paidResources.length === 0 &&
    catalog.bundles.length === 0;

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <div className="mb-8">
        <h1 className="font-display text-3xl font-bold text-foreground">
          Recursos
        </h1>
        <p className="mt-2 text-muted-foreground">
          Materiales didácticos para enseñar español. Descarga fichas, guías y
          ejercicios.
        </p>
      </div>

      {loading && <CatalogSkeleton />}

      {error && <p className="text-destructive">{error}</p>}

      {!loading && !error && isEmpty && (
        <div className="flex flex-col items-center justify-center py-20 text-center gap-3">
          <p className="text-4xl">📚</p>
          <h2 className="text-xl font-semibold">Próximamente</h2>
          <p className="text-muted-foreground max-w-sm">
            Paula está preparando materiales exclusivos para profesores de
            español. Vuelve pronto para descubrir los primeros recursos.
          </p>
        </div>
      )}

      {!loading && !error && catalog && !isEmpty && (
        <>
          <FreeResourcesSection
            resources={catalog.freeResources}
            onOpen={setDetailResource}
            onBuy={handleBuyResource}
          />

          {catalog.paidResources.length > 0 && (
            <section className="mb-10">
              <h2 className="text-xl font-semibold text-foreground mb-4">
                Recursos de pago
              </h2>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {catalog.paidResources.map((r) => (
                  <ResourceCardComponent
                    key={r.slug}
                    resource={r}
                    onOpen={() => setDetailResource(r)}
                    onBuy={() => handleBuyResource(r.slug)}
                  />
                ))}
              </div>
            </section>
          )}

          {catalog.bundles.length > 0 && (
            <section className="mb-10">
              <h2 className="text-xl font-semibold text-foreground mb-4">
                Packs
              </h2>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {catalog.bundles.map((b) => (
                  <BundleCardComponent
                    key={b.slug}
                    bundle={b}
                    onBuy={() => handleBuyBundle(b)}
                  />
                ))}
              </div>
            </section>
          )}
        </>
      )}

      <ResourceDetailDialog
        resource={detailResource}
        onClose={() => setDetailResource(null)}
        onBuy={(slug) => {
          setDetailResource(null);
          handleBuyResource(slug);
        }}
      />

      {purchaseTarget && (
        <PurchaseDialog
          target={purchaseTarget}
          onClose={() => setPurchaseTarget(null)}
          onSuccess={handlePurchaseSuccess}
        />
      )}
    </div>
  );
}
