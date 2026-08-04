import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getExerciseSubmissionResult,
  studentDisplayName,
  type ExerciseSubmissionResultAdmin,
} from "@/lib/admin";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { ExerciseResult } from "@/components/learning/ExerciseResult";

interface Props {
  submissionId: string;
  onClose: () => void;
}

/** Read-only view of a student's graded exercise answers for the teacher. */
export function ExerciseResultDialog({ submissionId, onClose }: Props) {
  const { t } = useTranslation();
  const [data, setData] = useState<ExerciseSubmissionResultAdmin | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setLoadError(null);
    getExerciseSubmissionResult(submissionId)
      .then(setData)
      .catch(() => setLoadError(t("admin.exerciseResult.loadError")))
      .finally(() => setLoading(false));
  }, [submissionId, t]);

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
            <div className="flex justify-end">
              <Button type="button" variant="outline" onClick={onClose}>
                {t("admin.exerciseResult.close")}
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
