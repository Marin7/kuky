import { useEffect, useState } from "react";
import { Link } from "@tanstack/react-router";
import { useTranslation } from "react-i18next";
import { markUnitSeen, notifyBadgesChanged } from "@/lib/notifications";
import { Skeleton } from "@/components/ui/skeleton";
import {
  getLearning,
  isPresentationPdf,
  type LearningResponse,
  type SharedPresentationSummary,
} from "@/lib/learning";
import { ActivityViewerPrompts } from "./ActivityViewerPrompts";
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

/** A unit's PDF, view-only (units are not downloadable), with its activities after their pages. */
function UnitPdf({
  presentation,
  onChanged,
}: {
  presentation: SharedPresentationSummary;
  onChanged: () => void;
}) {
  const { t } = useTranslation();
  const pdf = presentation.files.find((f) => isPresentationPdf(f.contentType));
  if (!pdf) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("learning.units.unitEmpty")}
      </p>
    );
  }
  return (
    <div className="rounded-md border bg-background p-2">
      <ActivityViewerPrompts
        presentationId={presentation.id}
        fileId={pdf.id}
        activities={presentation.activities ?? []}
        pageTitles={presentation.pageTitles}
        title={presentation.title}
        embedded
        onActivitiesChanged={onChanged}
      />
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

  useEffect(() => {
    if (unitId == null) return;
    markUnitSeen(unitId)
      .then(() => notifyBadgesChanged())
      .catch(() => {});
  }, [unitId]);

  const groups = data
    ? buildGroups(data.sharedPresentations, data.homework, data.unitQuizzes)
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
          ) : unitId !== null ? (
            // A unit opens straight on its PDF (activities sit after their pages).
            group.presentations.length > 0 ? (
              group.presentations.map((p) => (
                <UnitPdf key={p.id} presentation={p} onChanged={reload} />
              ))
            ) : (
              <p className="text-sm text-muted-foreground">
                {t("learning.units.unitEmpty")}
              </p>
            )
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
