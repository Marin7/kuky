import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { Link } from "@tanstack/react-router";
import { ResourceCard } from "./ResourceCard";
import { type ResourceCard as ResourceCardType } from "@/lib/resources";

interface FreeResourcesSectionProps {
  resources: ResourceCardType[];
  onOpen: (resource: ResourceCardType) => void;
  onBuy: (slug: string) => void;
}

export function FreeResourcesSection({
  resources,
  onOpen,
  onBuy,
}: FreeResourcesSectionProps) {
  const { t } = useTranslation();

  if (resources.length === 0) return null;

  return (
    <section className="mb-10">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-xl font-semibold text-foreground">
            {t("resources.free.title")}
          </h2>
          <p className="text-sm text-muted-foreground mt-0.5">
            {t("resources.free.subtitle")}
          </p>
        </div>
        <Button variant="outline" size="sm" asChild>
          <Link to="/reservas">{t("resources.free.bookCta")}</Link>
        </Button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {resources.map((r) => (
          <ResourceCard
            key={r.slug}
            resource={r}
            onOpen={() => onOpen(r)}
            onBuy={() => onBuy(r.slug)}
          />
        ))}
      </div>
    </section>
  );
}
