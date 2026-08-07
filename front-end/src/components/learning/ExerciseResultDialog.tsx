import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getExercise, type ExerciseResponse } from "@/lib/learning";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ExerciseResult } from "./ExerciseResult";

interface Props {
  homeworkId: string | null;
  onClose: () => void;
}

export function ExerciseResultDialog({ homeworkId, onClose }: Props) {
  const { t } = useTranslation();
  const [exercise, setExercise] = useState<ExerciseResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!homeworkId) {
      setExercise(null);
      setError(null);
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);
    setExercise(null);

    getExercise(homeworkId)
      .then((data) => {
        if (!cancelled) setExercise(data);
      })
      .catch(() => {
        if (!cancelled) setError(t("learning.resultDialog.loadError"));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [homeworkId, t]);

  return (
    <Dialog open={homeworkId !== null} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {exercise?.title ?? t("learning.homework.viewResult")}
          </DialogTitle>
        </DialogHeader>

        {loading && (
          <p className="animate-pulse text-sm text-muted-foreground">
            {t("learning.resultDialog.loading")}
          </p>
        )}

        {error && !exercise && (
          <p className="text-sm text-destructive">{error}</p>
        )}

        {exercise?.result && (
          <ExerciseResult
            questions={exercise.questions}
            result={exercise.result}
            teacherFeedback={exercise.teacherFeedback}
            instructions={
              exercise.homeworkType === "READ" ? exercise.instructions : null
            }
          />
        )}

        {exercise && !exercise.result && !loading && (
          <p className="text-sm text-muted-foreground">
            {t("learning.resultDialog.loadError")}
          </p>
        )}
      </DialogContent>
    </Dialog>
  );
}
