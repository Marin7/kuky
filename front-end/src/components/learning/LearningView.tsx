import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getLearning,
  type LearningResponse,
  type HomeworkItem,
} from "@/lib/learning";
import { Skeleton } from "@/components/ui/skeleton";
import { PastClassesList } from "./PastClassesList";
import { HomeworkSubmitDialog } from "./HomeworkSubmitDialog";
import { LearningContent } from "./LearningContent";

function LearningSkeleton() {
  return (
    <div className="space-y-10">
      <div className="space-y-3">
        <Skeleton className="h-6 w-52" />
        <Skeleton className="h-28 w-full rounded-xl" />
      </div>
      <div className="space-y-3">
        <Skeleton className="h-6 w-40" />
        <div className="grid gap-3 sm:grid-cols-2">
          <Skeleton className="h-20 w-full rounded-lg" />
          <Skeleton className="h-20 w-full rounded-lg" />
        </div>
      </div>
    </div>
  );
}

export function LearningView() {
  const { t } = useTranslation();
  const [data, setData] = useState<LearningResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [dialogItem, setDialogItem] = useState<HomeworkItem | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    getLearning()
      .then(setData)
      .catch(() => setError(t("learning.loadError")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

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

  return (
    <div className="mx-auto max-w-5xl px-6 py-10 space-y-10">
      {loading && <LearningSkeleton />}

      {error && <p className="text-destructive">{error}</p>}

      {!loading && !error && data && (
        <>
          <LearningContent
            presentations={data.sharedPresentations}
            homework={data.homework}
            onOpenHomework={setDialogItem}
          />
          <PastClassesList classes={data.pastClasses} />
        </>
      )}

      <HomeworkSubmitDialog
        item={dialogItem}
        onClose={() => setDialogItem(null)}
        onSubmitted={handleSubmitted}
      />
    </div>
  );
}
