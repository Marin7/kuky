import { useTranslation } from "react-i18next";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Lock, CheckCircle } from "lucide-react";
import {
  formatEur,
  type ResourceCard as ResourceCardType,
} from "@/lib/resources";

interface ResourceCardProps {
  resource: ResourceCardType;
  onOpen: () => void;
  onBuy: () => void;
}

export function ResourceCard({ resource, onOpen, onBuy }: ResourceCardProps) {
  const { t } = useTranslation();
  const {
    title,
    description,
    level,
    category,
    pricing,
    priceCents,
    owned,
    locked,
  } = resource;

  return (
    <div className="flex flex-col rounded-lg border border-border bg-card p-5 gap-3 hover:shadow-sm transition-shadow">
      <div className="flex items-start justify-between gap-2">
        <div className="flex gap-2 flex-wrap">
          {level && (
            <Badge variant="secondary" className="text-xs">
              {level}
            </Badge>
          )}
          {category && (
            <Badge variant="outline" className="text-xs">
              {category}
            </Badge>
          )}
        </div>
        <div className="shrink-0">
          {owned ? (
            <CheckCircle className="h-5 w-5 text-green-600" />
          ) : locked ? (
            <Lock className="h-5 w-5 text-muted-foreground" />
          ) : null}
        </div>
      </div>

      <div>
        <h3 className="font-semibold text-foreground">{title}</h3>
        <p className="text-sm text-muted-foreground mt-1 line-clamp-2">
          {description}
        </p>
      </div>

      <div className="flex items-center justify-between mt-auto pt-2">
        <span className="font-medium text-sm">
          {pricing === "FREE" ? (
            <span className="text-green-700">{t("resources.card.free")}</span>
          ) : priceCents != null ? (
            formatEur(priceCents)
          ) : (
            ""
          )}
        </span>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={onOpen}>
            {owned || pricing === "FREE"
              ? t("resources.card.view")
              : t("resources.card.preview")}
          </Button>
          {locked && (
            <Button size="sm" onClick={onBuy}>
              {t("resources.card.buy")}
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
