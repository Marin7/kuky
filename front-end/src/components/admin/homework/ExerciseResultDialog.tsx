import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getExerciseSubmissionResult,
  saveExerciseFeedback,
  studentDisplayName,
  type ApiError,
  type ExerciseSubmissionResultAdmin,
} from "@/lib/admin";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { ExerciseResult } from "@/components/learning/ExerciseResult";

interface Props {
  submissionId: string;
  onClose: () => void;
  /** Called after feedback is saved/cleared so parent lists can refresh indicators. */
  onFeedbackSaved?: () => void;
}

/** Graded exercise result for the teacher, with optional plain-text feedback. */
export function ExerciseResultDialog({
  submissionId,
  onClose,
  onFeedbackSaved,
}: Props) {
  const { t } = useTranslation();
  const [data, setData] = useState<ExerciseSubmissionResultAdmin | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [feedbackDraft, setFeedbackDraft] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setLoadError(null);
    getExerciseSubmissionResult(submissionId)
      .then((result) => {
        setData(result);
        setFeedbackDraft(result.teacherFeedback ?? "");
      })
      .catch(() => setLoadError(t("admin.exerciseResult.loadError")))
      .finally(() => setLoading(false));
  }, [submissionId, t]);

  const save = async () => {
    setSaving(true);
    setSaveError(null);
    try {
      const updated = await saveExerciseFeedback(submissionId, feedbackDraft);
      setData(updated);
      setFeedbackDraft(updated.teacherFeedback ?? "");
      onFeedbackSaved?.();
    } catch (e) {
      const err = e as ApiError;
      if (err.error === "VALIDATION_ERROR") {
        setSaveError(t("admin.exerciseResult.feedbackValidationError"));
      } else {
        setSaveError(t("admin.exerciseResult.feedbackSaveError"));
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[90vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {data
              ? data.assignmentTitle
              : t("admin.exerciseResult.dialogTitle")}
          </DialogTitle>
        </DialogHeader>

        {loading && (
          <p className="text-sm text-muted-foreground">
            {t("admin.exerciseResult.loading")}
          </p>
        )}
        {loadError && <p className="text-sm text-destructive">{loadError}</p>}

        {data && (
          <div className="space-y-4">
            <p className="text-sm text-muted-foreground">
              {studentDisplayName({
                firstName: data.studentFirstName,
                lastName: data.studentLastName,
                username: data.studentUsername,
                email: data.studentEmail,
              })}
            </p>
            <ExerciseResult
              questions={data.questions}
              result={data.result}
              showAllAnswers
            />
            <div className="space-y-2">
              <Label htmlFor="exercise-teacher-feedback">
                {t("admin.exerciseResult.feedbackLabel")}
              </Label>
              <Textarea
                id="exercise-teacher-feedback"
                value={feedbackDraft}
                onChange={(e) => setFeedbackDraft(e.target.value)}
                placeholder={t("admin.exerciseResult.feedbackPlaceholder")}
                rows={4}
                maxLength={2000}
              />
              {saveError && (
                <p className="text-sm text-destructive">{saveError}</p>
              )}
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={onClose}>
                {t("admin.exerciseResult.close")}
              </Button>
              <Button type="button" onClick={save} disabled={saving}>
                {saving
                  ? t("admin.exerciseResult.feedbackSaving")
                  : t("admin.exerciseResult.feedbackSave")}
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
