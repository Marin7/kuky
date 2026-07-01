import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "@tanstack/react-router";
import {
  getFullEvaluation,
  type FullEvaluationResponse,
} from "@/lib/placement";
import { Button } from "@/components/ui/button";
import { WritingComposer } from "./WritingComposer";

/**
 * The full-evaluation panel: a Writing submission form open to any logged-in
 * user (no payment gate — FR-010), and a link to book the evaluation
 * appointment through the normal booking flow (FR-011). The bank-transfer
 * conversation happens entirely offline, outside the app.
 */
export function FullEvaluationPanel() {
  const { t } = useTranslation();
  const [data, setData] = useState<FullEvaluationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    getFullEvaluation()
      .then(setData)
      .catch(() => setLoadError(t("placement.fullEvaluation.loadError")))
      .finally(() => setLoading(false));
  }, [t]);

  if (loading) {
    return (
      <p className="text-sm text-muted-foreground animate-pulse">
        {t("common.loading")}
      </p>
    );
  }

  if (loadError || !data) {
    return <p className="text-sm text-destructive">{loadError}</p>;
  }

  return (
    <div className="space-y-10">
      <h2 className="text-2xl font-semibold text-primary">
        {t("placement.fullEvaluation.title")}
      </h2>
      <p className="text-muted-foreground">
        {t("placement.fullEvaluation.intro")}
      </p>

      <div className="space-y-3">
        <h3 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
          {t("placement.fullEvaluation.writingTitle")}
        </h3>
        <p className="text-sm">{data.writingPrompt}</p>

        {data.mySubmission ? (
          <div className="rounded-lg border bg-muted/40 p-4 text-sm">
            <p className="mb-1 font-medium text-muted-foreground">
              {t("placement.fullEvaluation.yourSubmission")}
            </p>
            <p className="whitespace-pre-line">{data.mySubmission.body}</p>
          </div>
        ) : (
          <WritingComposer
            section={data.writingSection}
            onSubmitted={(submission) =>
              setData((prev) =>
                prev ? { ...prev, mySubmission: submission } : prev,
              )
            }
          />
        )}
      </div>

      <div className="space-y-3">
        <h3 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
          {t("placement.fullEvaluation.bookingTitle")}
        </h3>
        <p className="text-sm text-muted-foreground">
          {t("placement.fullEvaluation.bookingBody")}
        </p>
        <Button asChild variant="outline">
          <Link to="/reservas">{t("placement.fullEvaluation.bookingCta")}</Link>
        </Button>
      </div>
    </div>
  );
}
