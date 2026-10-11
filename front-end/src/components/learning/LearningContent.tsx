import { Link } from "@tanstack/react-router";
import { ChevronRight } from "lucide-react";
import { useTranslation } from "react-i18next";
import type {
  HomeworkItem,
  SharedPresentationSummary,
  UnitQuizSummary,
} from "@/lib/learning";
import { Card, CardContent } from "@/components/ui/card";
import { NotificationDot } from "@/components/NotificationDot";
import { UnitDetailContent } from "./UnitDetailContent";
import { buildGroups, type UnitGroup } from "./unitGroups";

interface Props {
  presentations: SharedPresentationSummary[];
  homework: HomeworkItem[];
  unitQuizzes: UnitQuizSummary[];
  onHomeworkChanged: () => void;
}

/** A unit card on Mi aprendizaje (homework is never part of a unit). */
function UnitCard({ group }: { group: UnitGroup }) {
  const { t } = useTranslation();
  const pendingCount = group.quizzes.filter(
    (q) => q.status === "AVAILABLE",
  ).length;

  return (
    <Link
      to="/aprendizaje/unidad/$unitId"
      params={{ unitId: group.unitId as string }}
      className="block rounded-xl outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      <Card className="transition-colors hover:bg-muted/40">
        <CardContent className="flex items-center justify-between gap-3 pt-4">
          <div className="min-w-0 space-y-1.5">
            <p className="inline-flex max-w-full items-center gap-1.5 truncate font-medium text-foreground">
              <span className="truncate">{group.label}</span>
              {group.unseen && (
                <NotificationDot label={t("notification.item")} />
              )}
            </p>
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
              {group.presentations.length > 0 && (
                <span>
                  {group.presentations.length === 1
                    ? t("learning.units.presentationsCountSingular", {
                        count: group.presentations.length,
                      })
                    : t("learning.units.presentationsCountPlural", {
                        count: group.presentations.length,
                      })}
                </span>
              )}
              {group.quizzes.length > 0 && (
                <span>
                  {t("learning.units.quizCount", {
                    count: group.quizzes.length,
                  })}
                </span>
              )}
              {pendingCount > 0 && (
                <span className="inline-block rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">
                  {t("learning.units.pendingBadge")}
                </span>
              )}
            </div>
          </div>
          <ChevronRight
            className="h-5 w-5 shrink-0 text-muted-foreground"
            aria-hidden
          />
          <span className="sr-only">{t("learning.units.openUnit")}</span>
        </CardContent>
      </Card>
    </Link>
  );
}

/** "Tus unidades" (unit cards) and "Otros materiales" (homework + directly shared presentations, inline). */
export function LearningContent({
  presentations,
  homework,
  unitQuizzes,
  onHomeworkChanged,
}: Props) {
  const { t } = useTranslation();
  const groups = buildGroups(presentations, homework, unitQuizzes);
  const units = groups.filter((g) => g.unitId !== null);
  const other = groups.find((g) => g.unitId === null);

  return (
    <>
      <section className="space-y-4">
        <h2 className="font-display text-xl font-bold text-foreground">
          {t("learning.units.title")}
        </h2>
        {units.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            {t("learning.units.empty")}
          </p>
        ) : (
          <div className="space-y-3">
            {units.map((group) => (
              <UnitCard key={group.key} group={group} />
            ))}
          </div>
        )}
      </section>

      {other && (
        <section className="space-y-4">
          <h2 className="font-display text-xl font-bold text-foreground">
            {t("learning.units.other")}
          </h2>
          {/* Grouped by kind rather than one mixed list. */}
          {other.homework.length > 0 && (
            <div className="space-y-2">
              <h3 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
                {t("learning.units.homework")}
              </h3>
              <UnitDetailContent
                presentations={[]}
                homework={other.homework}
                onHomeworkChanged={onHomeworkChanged}
              />
            </div>
          )}
          {other.presentations.length > 0 && (
            <div className="space-y-2">
              <h3 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
                {t("learning.units.presentations")}
              </h3>
              <UnitDetailContent
                presentations={other.presentations}
                homework={[]}
                onHomeworkChanged={onHomeworkChanged}
              />
            </div>
          )}
        </section>
      )}
    </>
  );
}
