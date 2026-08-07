import { Link } from "@tanstack/react-router";
import { ChevronRight } from "lucide-react";
import { useTranslation } from "react-i18next";
import type { HomeworkItem, SharedPresentationSummary } from "@/lib/learning";
import { Card, CardContent } from "@/components/ui/card";
import { buildGroups, type UnitGroup } from "./unitGroups";

interface Props {
  presentations: SharedPresentationSummary[];
  homework: HomeworkItem[];
}

function UnitCardBody({ group }: { group: UnitGroup }) {
  const { t } = useTranslation();
  const pendingCount = group.homework.filter(
    (h) => h.status === "PENDING",
  ).length;
  const title = group.label ?? t("learning.units.other");

  return (
    <Card className="transition-colors hover:bg-muted/40">
      <CardContent className="flex items-center justify-between gap-3 pt-4">
        <div className="min-w-0 space-y-1.5">
          <p className="truncate font-medium text-foreground">{title}</p>
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
            {group.homework.length > 0 && (
              <span>
                {group.homework.length === 1
                  ? t("learning.units.homeworkCountSingular", {
                      count: group.homework.length,
                    })
                  : t("learning.units.homeworkCountPlural", {
                      count: group.homework.length,
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
  );
}

function UnitCard({ group }: { group: UnitGroup }) {
  const linkClass =
    "block rounded-xl outline-none focus-visible:ring-2 focus-visible:ring-ring";

  if (group.unitId === null) {
    return (
      <Link to="/aprendizaje/otros" className={linkClass}>
        <UnitCardBody group={group} />
      </Link>
    );
  }

  return (
    <Link
      to="/aprendizaje/unidad/$unitId"
      params={{ unitId: group.unitId }}
      className={linkClass}
    >
      <UnitCardBody group={group} />
    </Link>
  );
}

export function LearningContent({ presentations, homework }: Props) {
  const { t } = useTranslation();
  const groups = buildGroups(presentations, homework);

  return (
    <section className="space-y-4">
      <h2 className="font-display text-xl font-bold text-foreground">
        {t("learning.units.title")}
      </h2>

      {groups.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {t("learning.units.empty")}
        </p>
      ) : (
        <div className="space-y-3">
          {groups.map((group) => (
            <UnitCard key={group.key} group={group} />
          ))}
        </div>
      )}
    </section>
  );
}
