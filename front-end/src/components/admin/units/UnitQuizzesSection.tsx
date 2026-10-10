import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  listAdminQuizzes,
  setUnitQuizzes,
  type ApiError,
  type QuizAdminListItem,
  type UnitDetail,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { AddContentCombobox } from "./AddContentCombobox";

interface Props {
  unit: UnitDetail;
  onChanged: (detail: UnitDetail) => void;
}

/**
 * Quizzes shown at the end of the unit, in order. The unit's students get them; adding a
 * quiz that belongs to another unit moves it here.
 */
export function UnitQuizzesSection({ unit, onChanged }: Props) {
  const { t } = useTranslation();
  const [allQuizzes, setAllQuizzes] = useState<QuizAdminListItem[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listAdminQuizzes()
      .then(setAllQuizzes)
      .catch(() => setError(t("admin.units.quizzes.loadError")));
  }, [t]);

  const ids = unit.quizzes.map((q) => q.id);

  const save = async (next: string[]) => {
    setSaving(true);
    setError(null);
    try {
      onChanged(await setUnitQuizzes(unit.id, next));
      setAllQuizzes(await listAdminQuizzes());
    } catch (err) {
      setError((err as ApiError).message ?? t("admin.units.quizzes.saveError"));
    } finally {
      setSaving(false);
    }
  };

  const move = (index: number, delta: number) => {
    const next = [...ids];
    const target = index + delta;
    [next[index], next[target]] = [next[target], next[index]];
    void save(next);
  };

  const available = allQuizzes.filter(
    (q) => !ids.includes(q.id) && q.questionCount > 0,
  );

  return (
    <div className="space-y-3">
      {unit.quizzes.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {t("admin.units.quizzes.empty")}
        </p>
      ) : (
        <ol className="divide-y rounded-md border">
          {unit.quizzes.map((q, i) => (
            <li
              key={q.id}
              className="flex flex-wrap items-center gap-2 px-3 py-2 text-sm"
            >
              <span className="w-5 shrink-0 text-muted-foreground">
                {i + 1}.
              </span>
              <span className="min-w-0 flex-1 truncate font-medium">
                {q.title}
              </span>
              <span className="text-xs text-muted-foreground">
                {t("admin.units.quizzes.questionCount", {
                  count: q.questionCount,
                })}
              </span>
              <Button
                variant="ghost"
                size="sm"
                className="h-7 px-2"
                disabled={saving || i === 0}
                onClick={() => move(i, -1)}
                aria-label={t("admin.units.quizzes.moveUp")}
              >
                ▲
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="h-7 px-2"
                disabled={saving || i === unit.quizzes.length - 1}
                onClick={() => move(i, 1)}
                aria-label={t("admin.units.quizzes.moveDown")}
              >
                ▼
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="h-7 px-2 text-destructive"
                disabled={saving}
                onClick={() => save(ids.filter((id) => id !== q.id))}
              >
                {t("admin.units.quizzes.remove")}
              </Button>
            </li>
          ))}
        </ol>
      )}
      {available.length > 0 && (
        <AddContentCombobox
          triggerLabel={t("admin.units.quizzes.add")}
          searchPlaceholder={t("admin.units.quizzes.search")}
          emptyLabel={t("admin.units.quizzes.noMatches")}
          options={available.map((q) => ({
            id: q.id,
            title: q.unitLabel
              ? t("admin.units.quizzes.inOtherUnit", {
                  title: q.title,
                  unit: q.unitLabel,
                })
              : q.title,
          }))}
          onSelect={(id) => save([...ids, id])}
        />
      )}
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}
