import { useTranslation } from "react-i18next";
import { Link } from "@tanstack/react-router";
import { Button } from "@/components/ui/button";

/** Shown to anonymous visitors instead of silently redirecting away (FR-001). */
export function PlacementLoginRequired() {
  const { t } = useTranslation();

  return (
    <div className="space-y-6 text-center">
      <h1 className="font-display text-3xl font-semibold text-primary">
        {t("placement.intro.title")}
      </h1>
      <p className="mx-auto max-w-xl text-muted-foreground">
        {t("placement.intro.loginRequired")}
      </p>
      <Button asChild size="lg">
        <Link to="/cuenta">{t("placement.intro.goToLogin")}</Link>
      </Button>
    </div>
  );
}
