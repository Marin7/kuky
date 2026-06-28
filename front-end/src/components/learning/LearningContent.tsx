import { useState } from "react";
import { useTranslation } from "react-i18next";
import type { HomeworkItem, SharedPresentationSummary } from "@/lib/learning";
import { downloadPresentation } from "@/lib/learning";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { HomeworkItemCard } from "./HomeworkItemCard";

interface Props {
  presentations: SharedPresentationSummary[];
  homework: HomeworkItem[];
  onOpenHomework: (item: HomeworkItem) => void;
}

interface UnitGroup {
  key: string;
  level: string | null;
  label: string | null; // null → "Other" bucket
  position: number;
  presentations: SharedPresentationSummary[];
  homework: HomeworkItem[];
}

const STATUS_ORDER: Record<string, number> = {
  PENDING: 0,
  SUBMITTED: 1,
  REVIEWED: 2,
  GRADED: 3,
};

const OTHER_KEY = "__other__";

function buildGroups(
  presentations: SharedPresentationSummary[],
  homework: HomeworkItem[],
): UnitGroup[] {
  const map = new Map<string, UnitGroup>();
  const other: UnitGroup = {
    key: OTHER_KEY,
    level: null,
    label: null,
    position: Number.MAX_SAFE_INTEGER,
    presentations: [],
    homework: [],
  };

  const ensure = (unit: {
    level: string;
    subject: string;
    position: number;
  }) => {
    const key = `${unit.level}::${unit.subject}::${unit.position}`;
    let g = map.get(key);
    if (!g) {
      g = {
        key,
        level: unit.level,
        label: `${unit.level} · ${unit.subject}`,
        position: unit.position,
        presentations: [],
        homework: [],
      };
      map.set(key, g);
    }
    return g;
  };

  for (const p of presentations) {
    if (p.unit) ensure(p.unit).presentations.push(p);
    else other.presentations.push(p);
  }
  for (const h of homework) {
    if (h.unit) ensure(h.unit).homework.push(h);
    else other.homework.push(h);
  }

  const groups = [...map.values()].sort((a, b) => {
    const lvl = (a.level ?? "").localeCompare(b.level ?? "");
    return lvl !== 0 ? lvl : a.position - b.position;
  });
  if (other.presentations.length > 0 || other.homework.length > 0) {
    groups.push(other);
  }
  return groups;
}

function PresentationDownloadCard({
  presentation,
}: {
  presentation: SharedPresentationSummary;
}) {
  const { t } = useTranslation();
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleDownload = async () => {
    setDownloading(true);
    setError(null);
    try {
      await downloadPresentation(presentation.id, `${presentation.title}.pptx`);
    } catch {
      setError(t("learning.presentations.loadError"));
    } finally {
      setDownloading(false);
    }
  };

  return (
    <Card>
      <CardContent className="flex items-center justify-between pt-4">
        <p className="font-medium truncate">{presentation.title}</p>
        {presentation.hasFile ? (
          <Button
            variant="outline"
            size="sm"
            className="shrink-0 ml-3"
            disabled={downloading}
            onClick={handleDownload}
          >
            {downloading
              ? t("learning.presentations.downloading")
              : t("learning.presentations.download")}
          </Button>
        ) : (
          <span className="text-xs text-muted-foreground shrink-0 ml-3">
            {t("learning.presentations.noFile")}
          </span>
        )}
        {error && <p className="w-full text-sm text-destructive">{error}</p>}
      </CardContent>
    </Card>
  );
}

export function LearningContent({
  presentations,
  homework,
  onOpenHomework,
}: Props) {
  const { t } = useTranslation();
  const groups = buildGroups(presentations, homework);

  return (
    <section className="space-y-8">
      <h2 className="font-display text-xl font-bold text-foreground">
        {t("learning.units.title")}
      </h2>

      {groups.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {t("learning.units.empty")}
        </p>
      ) : (
        groups.map((group) => (
          <div key={group.key} className="space-y-4">
            <h3 className="text-base font-semibold text-foreground">
              {group.label ?? t("learning.units.other")}
            </h3>

            {group.presentations.length > 0 && (
              <div className="space-y-2">
                <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  {t("learning.units.presentations")}
                </p>
                <div className="grid gap-3 sm:grid-cols-2">
                  {group.presentations.map((p) => (
                    <PresentationDownloadCard key={p.id} presentation={p} />
                  ))}
                </div>
              </div>
            )}

            {group.homework.length > 0 && (
              <div className="space-y-2">
                <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  {t("learning.units.homework")}
                </p>
                <div className="space-y-3">
                  {[...group.homework]
                    .sort(
                      (a, b) =>
                        (STATUS_ORDER[a.status] ?? 9) -
                        (STATUS_ORDER[b.status] ?? 9),
                    )
                    .map((item) => (
                      <HomeworkItemCard
                        key={item.id}
                        item={item}
                        onOpen={onOpenHomework}
                      />
                    ))}
                </div>
              </div>
            )}
          </div>
        ))
      )}
    </section>
  );
}
