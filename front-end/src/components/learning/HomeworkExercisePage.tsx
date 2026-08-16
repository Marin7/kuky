import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "@tanstack/react-router";
import {
  getExercise,
  pinInstructionsAboveWordBank,
  resolveComposition,
  type ExerciseResponse,
} from "@/lib/learning";
import { notifyBadgesChanged } from "@/lib/notifications";
import { ExerciseForm } from "./ExerciseForm";
import { MixedHomeworkForm } from "./MixedHomeworkForm";
import { HomeworkDueOn } from "./HomeworkDueOn";

interface Props {
  homeworkId: string;
}

export function HomeworkExercisePage({ homeworkId }: Props) {
  const { t } = useTranslation();
  const [exercise, setExercise] = useState<ExerciseResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getExercise(homeworkId)
      .then((data) => {
        setExercise(data);
        notifyBadgesChanged();
      })
      .catch(() => setError(t("learning.exercisePage.loadError")))
      .finally(() => setLoading(false));
  }, [homeworkId, t]);

  const composition = exercise ? resolveComposition(exercise) : null;

  return (
    <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 sm:py-8">
      <Link
        to="/aprendizaje"
        className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        {t("learning.exercisePage.back")}
      </Link>

      {loading && (
        <p className="mt-4 animate-pulse text-sm text-muted-foreground">
          {t("learning.exercisePage.loading")}
        </p>
      )}

      {error && !exercise && (
        <p className="mt-4 text-sm text-destructive">{error}</p>
      )}

      {exercise && (
        <>
          <h1 className="font-display text-2xl font-semibold text-primary sm:text-3xl">
            {exercise.title}
          </h1>
          <HomeworkDueOn
            dueOn={exercise.dueOn}
            className="mt-2 block text-sm text-muted-foreground"
          />
          {exercise.instructions &&
            !pinInstructionsAboveWordBank(
              exercise.questions,
              exercise.homeworkType,
              exercise.status,
            ) && (
              <p className="mt-2 whitespace-pre-wrap text-base leading-relaxed text-muted-foreground">
                {exercise.instructions}
              </p>
            )}

          {composition === "MIXED" ? (
            <MixedHomeworkForm
              assignment={{
                id: exercise.id,
                status: exercise.status,
                questions: exercise.questions,
                result: exercise.result,
                answers: exercise.answers,
                scorePercent: exercise.scorePercent ?? null,
                provisionalScorePercent: exercise.provisionalScorePercent,
                feedbackText: exercise.feedbackText,
                teacherFeedback: exercise.teacherFeedback,
                contentRevisedAt: exercise.contentRevisedAt,
                instructions: exercise.instructions,
                homeworkType: exercise.homeworkType,
              }}
              onSubmitted={() =>
                getExercise(homeworkId)
                  .then(setExercise)
                  .catch(() => {})
              }
              onHomeworkUpdated={() =>
                getExercise(homeworkId)
                  .then(setExercise)
                  .catch(() => {})
              }
            />
          ) : (
            <ExerciseForm
              exercise={exercise}
              onHomeworkUpdated={() =>
                getExercise(homeworkId)
                  .then(setExercise)
                  .catch(() => {})
              }
            />
          )}
        </>
      )}
    </div>
  );
}
