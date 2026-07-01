import { useTranslation } from "react-i18next";
import type { AttemptResultResponse } from "@/lib/placement";
import { Button } from "@/components/ui/button";

interface Props {
  result: AttemptResultResponse;
  onWantFullEvaluation: () => void;
}

function levelLabel(
  level: string,
  t: ReturnType<typeof useTranslation>["t"],
): string {
  return level === "A0" ? t("placement.result.below") : level;
}

/** Per-skill CEFR breakdown + overall estimate, with a CTA into the full evaluation (FR-004/FR-008). */
export function PlacementResult({ result, onWantFullEvaluation }: Props) {
  const { t } = useTranslation();

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl font-semibold text-primary">
          {t("placement.result.title")}
        </h2>
        {result.overallCefr && (
          <p className="mt-2 text-lg">
            {t("placement.result.overall")}:{" "}
            <span className="font-semibold">
              {levelLabel(result.overallCefr, t)}
            </span>
          </p>
        )}
      </div>

      <div>
        <h3 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
          {t("placement.result.perSkill")}
        </h3>
        <div className="mt-3 grid grid-cols-1 gap-4 sm:grid-cols-3">
          {result.skills.map((s) => (
            <div
              key={s.skill}
              className="rounded-lg border bg-card p-4 text-center"
            >
              <p className="text-xs font-medium uppercase text-muted-foreground">
                {t(`placement.intro.sections.${s.skill}` as never)}
              </p>
              <p className="mt-1 text-2xl font-semibold">
                {levelLabel(s.cefrLevel, t)}
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                {t("placement.result.scoreLabel", { score: s.scorePercent })}
              </p>
            </div>
          ))}
        </div>
      </div>

      <Button onClick={onWantFullEvaluation}>
        {t("placement.result.cta")}
      </Button>
    </div>
  );
}
