import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  createPageActivity,
  deleteActivity,
  getActivityAdmin,
  updateActivity,
  type AdminQuestion,
  type ApiError,
  type UnitActivitySummary,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { QuestionListEditor } from "@/components/admin/homework/QuestionListEditor";

interface Props {
  unitId: string;
  page: number;
  /** The page's activity, if the page is already marked. */
  activity: UnitActivitySummary | undefined;
  onChanged: () => void;
}

/** Mark / edit / unmark the selected PDF page as an activity. The page is the instructions. */
export function PageActivityEditor({
  unitId,
  page,
  activity,
  onChanged,
}: Props) {
  const { t } = useTranslation();
  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState("");
  const [questions, setQuestions] = useState<AdminQuestion[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setError(null);
    setEditing(false);
    setTitle("");
    setQuestions([]);
    if (!activity) return;
    let cancelled = false;
    setLoading(true);
    getActivityAdmin(activity.id)
      .then((a) => {
        if (cancelled) return;
        setTitle(a.title ?? "");
        setQuestions(a.questions ?? []);
        setEditing(true);
      })
      .catch(
        () => !cancelled && setError(t("admin.units.editor.loadActivityError")),
      )
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [activity?.id, page, t]);

  const handleSave = async () => {
    if (questions.length === 0) {
      setError(t("admin.homework.editor.questionsRequired"));
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const fields = { title: title.trim(), questions };
      if (activity) {
        await updateActivity(activity.id, fields);
      } else {
        await createPageActivity(unitId, page, fields);
      }
      onChanged();
    } catch (err) {
      const e = err as ApiError;
      setError(
        e.error === "ACTIVITY_PAGE_TAKEN"
          ? t("admin.units.editor.pageTaken")
          : (e.message ?? t("admin.units.editor.saveActivityError")),
      );
    } finally {
      setSaving(false);
    }
  };

  const handleUnmark = async () => {
    if (!activity) {
      setEditing(false);
      return;
    }
    if (!window.confirm(t("admin.units.editor.confirmUnmark", { page })))
      return;
    setError(null);
    try {
      await deleteActivity(activity.id);
      onChanged();
    } catch (err) {
      const e = err as ApiError;
      if (e.error !== "ACTIVITY_HAS_SUBMISSIONS") {
        setError(e.message ?? t("admin.units.editor.saveActivityError"));
        return;
      }
      if (
        !window.confirm(t("admin.units.editor.confirmUnmarkWithSubmissions"))
      ) {
        return;
      }
      try {
        await deleteActivity(activity.id, true);
        onChanged();
      } catch (retry) {
        setError(
          (retry as ApiError).message ??
            t("admin.units.editor.saveActivityError"),
        );
      }
    }
  };

  if (loading) {
    return (
      <p className="animate-pulse text-sm text-muted-foreground">
        {t("admin.units.loading")}
      </p>
    );
  }

  if (!editing) {
    return (
      <div className="space-y-2 rounded-md border border-dashed p-4">
        <p className="text-sm text-muted-foreground">
          {t("admin.units.editor.pageNotActivity", { page })}
        </p>
        <Button size="sm" onClick={() => setEditing(true)}>
          {t("admin.units.editor.markAsActivity")}
        </Button>
        {error && <p className="text-xs text-destructive">{error}</p>}
      </div>
    );
  }

  return (
    <div className="space-y-4 rounded-md border border-primary/30 bg-primary/5 p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm font-semibold">
          {t("admin.units.editor.activityOnPage", { page })}
        </p>
        <Button
          size="sm"
          variant="ghost"
          className="text-destructive"
          onClick={handleUnmark}
        >
          {activity ? t("admin.units.editor.unmark") : t("common.cancel")}
        </Button>
      </div>
      <div className="space-y-1">
        <Label htmlFor={`activity-title-${page}`}>
          {t("admin.units.editor.titleLabel")}
        </Label>
        <Input
          id={`activity-title-${page}`}
          value={title}
          maxLength={200}
          placeholder={t("learning.activities.pageTitle", { page })}
          onChange={(e) => setTitle(e.target.value)}
        />
      </div>
      <QuestionListEditor questions={questions} onChange={setQuestions} />
      {error && <p className="text-sm text-destructive">{error}</p>}
      <div className="flex justify-end">
        <Button onClick={handleSave} disabled={saving}>
          {saving ? t("common.saving") : t("common.save")}
        </Button>
      </div>
    </div>
  );
}
