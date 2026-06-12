import { useTranslation } from "react-i18next";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CheckCircle, Package } from "lucide-react";
import { formatEur, type BundleCard as BundleCardType } from "@/lib/resources";

interface BundleCardProps {
  bundle: BundleCardType;
  onBuy: () => void;
}

export function BundleCard({ bundle, onBuy }: BundleCardProps) {
  const { t } = useTranslation();
  const { title, description, priceCents, resourceSlugs, owned } = bundle;

  return (
    <div className="flex flex-col rounded-lg border border-primary/30 bg-primary/5 p-5 gap-3">
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2">
          <Package className="h-4 w-4 text-primary" />
          <Badge variant="default" className="text-xs">
            {t("resources.bundle.badge")}
          </Badge>
        </div>
        {owned && <CheckCircle className="h-5 w-5 text-green-600 shrink-0" />}
      </div>

      <div>
        <h3 className="font-semibold text-foreground">{title}</h3>
        <p className="text-sm text-muted-foreground mt-1">{description}</p>
      </div>

      {resourceSlugs.length > 0 && (
        <ul className="text-xs text-muted-foreground space-y-0.5 list-disc list-inside">
          {resourceSlugs.map((slug) => (
            <li key={slug}>{slug.replace(/-/g, " ")}</li>
          ))}
        </ul>
      )}

      <div className="flex items-center justify-between mt-auto pt-2">
        <span className="font-semibold">{formatEur(priceCents)}</span>
        {owned ? (
          <span className="text-sm text-green-700 font-medium">
            {t("resources.bundle.acquired")}
          </span>
        ) : (
          <Button size="sm" onClick={onBuy}>
            {t("resources.bundle.buy")}
          </Button>
        )}
      </div>
    </div>
  );
}
