import { useEffect, useState } from "react";
import { Link } from "@tanstack/react-router";
import { useTranslation } from "react-i18next";
import {
  getLearning,
  type LearningResponse,
  type HomeworkItem,
} from "@/lib/learning";
import { Skeleton } from "@/components/ui/skeleton";
import { HomeworkSubmitDialog } from "./HomeworkSubmitDialog";
import { ExerciseResultDialog } from "./ExerciseResultDialog";
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
  const [dialogItem, setDialogItem] = useState<HomeworkItem | null>(null);
  const [resultHomeworkId, setResultHomeworkId] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    getLearning()
      .then(setData)
      .catch(() => setError(t("learning.loadError")))
      .finally(() => setLoading(false));
  }, [t]);

  const handleSubmitted = (updated: HomeworkItem) => {
    setData((prev) =>
      prev
        ? {
            ...prev,
            homework: prev.homework.map((h) =>
              h.id === updated.id ? updated : h,
            ),
          }
        : prev,
    );
  };

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
              onOpenHomework={setDialogItem}
              onViewResult={(item) => setResultHomeworkId(item.id)}
            />
          )}
        </>
      )}

      <HomeworkSubmitDialog
        item={dialogItem}
        onClose={() => setDialogItem(null)}
        onSubmitted={handleSubmitted}
      />

      <ExerciseResultDialog
        homeworkId={resultHomeworkId}
        onClose={() => setResultHomeworkId(null)}
      />
    </div>
  );
}
