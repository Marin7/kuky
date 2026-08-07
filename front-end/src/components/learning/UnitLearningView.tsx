import { useEffect, useState } from "react";
import { Link } from "@tanstack/react-router";
import { useTranslation } from "react-i18next";
import { getLearning, type LearningResponse } from "@/lib/learning";
import { Skeleton } from "@/components/ui/skeleton";
import { UnitDetailContent } from "./UnitDetailContent";
import { buildGroups, findUnitGroup } from "./unitGroups";

interface Props {
  /** Null selects the unattached "Otros" bucket. */
  unitId: string | null;
}

function UnitSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-5 w-48" />
      <Skeleton className="h-8 w-64" />
      <div className="grid gap-3 sm:grid-cols-2">
        <Skeleton className="h-24 w-full rounded-lg" />
        <Skeleton className="h-24 w-full rounded-lg" />
      </div>
      <Skeleton className="h-20 w-full rounded-lg" />
    </div>
  );
}

export function UnitLearningView({ unitId }: Props) {
  const { t } = useTranslation();
  const [data, setData] = useState<LearningResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = () => {
    getLearning()
      .then(setData)
      .catch(() => setError(t("learning.loadError")));
  };

  useEffect(() => {
    setLoading(true);
    setError(null);
    getLearning()
      .then(setData)
      .catch(() => setError(t("learning.loadError")))
      .finally(() => setLoading(false));
  }, [t]);

  const groups = data
    ? buildGroups(data.sharedPresentations, data.homework)
    : [];
  const group = data ? findUnitGroup(groups, unitId) : undefined;
  const title =
    unitId === null
      ? t("learning.units.other")
      : (group?.label ?? t("learning.units.notFound"));

  return (
    <div className="mx-auto max-w-5xl space-y-8 px-6 py-10">
      <Link
        to="/aprendizaje"
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        {t("learning.units.back")}
      </Link>

      {loading && <UnitSkeleton />}

      {error && <p className="text-destructive">{error}</p>}

      {!loading && !error && data && (
        <>
          <h1 className="font-display text-2xl font-bold text-foreground">
            {title}
          </h1>

          {!group ? (
            <p className="text-sm text-muted-foreground">
              {t("learning.units.notFound")}
            </p>
          ) : (
            <UnitDetailContent
              presentations={group.presentations}
              homework={group.homework}
              onHomeworkChanged={reload}
            />
          )}
        </>
      )}
    </div>
  );
}
