import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Lock, FileDown, Video } from "lucide-react";
import {
  getResourceContent,
  formatEur,
  type ResourceCard,
  type ResourceDetail,
  type AssetItem,
  type ApiError,
} from "@/lib/resources";
import { Link } from "@tanstack/react-router";

interface ResourceDetailDialogProps {
  resource: ResourceCard | ResourceDetail | null;
  onClose: () => void;
  onBuy: (slug: string) => void;
}

export function ResourceDetailDialog({
  resource,
  onClose,
  onBuy,
}: ResourceDetailDialogProps) {
  const { t } = useTranslation();
  const [assets, setAssets] = useState<AssetItem[] | null>(null);
  const [contentError, setContentError] = useState<string | null>(null);

  useEffect(() => {
    if (!resource) {
      setAssets(null);
      setContentError(null);
      return;
    }

    if (!resource.locked) {
      setAssets(null);
      setContentError(null);
      getResourceContent(resource.slug)
        .then((c) => setAssets(c.assets))
        .catch((e: ApiError) =>
          setContentError(e.message ?? t("resources.loadError")),
        );
    }
  }, [resource?.slug, resource?.locked]);

  const previewText =
    resource && "previewText" in resource ? resource.previewText : null;

  return (
    <Dialog open={!!resource} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-lg">
        {resource && (
          <>
            <DialogHeader>
              <div className="flex flex-wrap gap-2 mb-1">
                {resource.level && (
                  <Badge variant="secondary" className="text-xs">
                    {resource.level}
                  </Badge>
                )}
                {resource.category && (
                  <Badge variant="outline" className="text-xs">
                    {resource.category}
                  </Badge>
                )}
                {resource.pricing === "FREE" && (
                  <Badge className="text-xs bg-green-100 text-green-800 border-green-200">
                    {t("resources.detail.freeBadge")}
                  </Badge>
                )}
              </div>
              <DialogTitle>{resource.title}</DialogTitle>
            </DialogHeader>

            <div className="space-y-4 py-2">
              <p className="text-sm text-muted-foreground">
                {resource.description}
              </p>

              {previewText && (
                <div className="rounded-md bg-muted p-3 text-sm">
                  <p className="font-medium text-xs uppercase tracking-wide text-muted-foreground mb-1">
                    {t("resources.detail.previewLabel")}
                  </p>
                  <p>{previewText}</p>
                </div>
              )}

              {resource.locked ? (
                <div className="flex flex-col items-center gap-3 py-4 text-center">
                  <Lock className="h-8 w-8 text-muted-foreground" />
                  <p className="text-sm text-muted-foreground">
                    {t("resources.detail.lockedMessage")}
                  </p>
                  <div className="flex items-center gap-2">
                    {resource.priceCents != null && (
                      <span className="font-semibold">
                        {formatEur(resource.priceCents)}
                      </span>
                    )}
                    <Button onClick={() => onBuy(resource.slug)}>
                      {t("resources.detail.buy")}
                    </Button>
                  </div>
                </div>
              ) : assets === null && !contentError ? (
                <p className="text-sm text-muted-foreground animate-pulse">
                  {t("resources.detail.loadingMaterials")}
                </p>
              ) : contentError ? (
                <p className="text-sm text-destructive">{contentError}</p>
              ) : assets && assets.length > 0 ? (
                <div className="space-y-2">
                  <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                    Materiales
                  </p>
                  {assets.map((asset, i) => (
                    <a
                      key={i}
                      href={asset.locator}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex items-center gap-2 rounded-md border border-border p-3 text-sm hover:bg-accent/30 transition-colors"
                    >
                      {asset.assetType === "FILE" ? (
                        <FileDown className="h-4 w-4 text-muted-foreground shrink-0" />
                      ) : (
                        <Video className="h-4 w-4 text-muted-foreground shrink-0" />
                      )}
                      <span>{asset.label}</span>
                    </a>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">
                  {t("resources.detail.noMaterials")}
                </p>
              )}

              {!resource.locked && resource.pricing === "FREE" && (
                <div className="border-t border-border pt-4 space-y-2">
                  {resource.relatedResourceSlug && (
                    <Button
                      className="w-full"
                      onClick={() => {
                        onClose();
                        onBuy(resource.relatedResourceSlug!);
                      }}
                    >
                      {t("resources.detail.unlockFull")}
                    </Button>
                  )}
                  <Button variant="outline" className="w-full" asChild>
                    <Link to="/reservas">
                      {t("resources.detail.bookClass")}
                    </Link>
                  </Button>
                </div>
              )}
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
