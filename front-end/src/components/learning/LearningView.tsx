import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getLearning, type LearningResponse } from "@/lib/learning";
import { Skeleton } from "@/components/ui/skeleton";
import { PastClassesList } from "./PastClassesList";
import { LearningContent } from "./LearningContent";
import { MyTestimonial } from "./MyTestimonial";

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

  useEffect(() => {
    setLoading(true);
    setError(null);
    getLearning()
      .then(setData)
      .catch(() => setError(t("learning.loadError")))
      .finally(() => setLoading(false));
  }, [t]);

  return (
    <div className="mx-auto max-w-5xl space-y-10 px-6 py-10">
      {loading && <LearningSkeleton />}

      {error && <p className="text-destructive">{error}</p>}

      {!loading && !error && data && (
        <>
          <LearningContent
            presentations={data.sharedPresentations}
            homework={data.homework}
          />
          <PastClassesList classes={data.pastClasses} />
          <MyTestimonial />
        </>
      )}
    </div>
  );
}
