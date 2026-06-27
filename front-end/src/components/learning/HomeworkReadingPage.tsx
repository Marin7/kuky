import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "@tanstack/react-router";
import {
  getExercise,
  getLearning,
  type ExerciseResponse,
  type HomeworkItem,
  type HomeworkFormat,
} from "@/lib/learning";
import { ExerciseForm } from "./ExerciseForm";
import { ManualAnswerForm } from "./ManualAnswerForm";

interface Props {
  homeworkId: string;
  format: HomeworkFormat;
}

/**
 * Reading ("Lectura") homework on its own page: the passage is shown
 * prominently on top, with the questions (EXERCISE) or a free-text answer
 * (MANUAL) on separate lines below.
 */
export function HomeworkReadingPage({ homeworkId, format }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [exercise, setExercise] = useState<ExerciseResponse | null>(null);
  const [item, setItem] = useState<HomeworkItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    const load =
      format === "EXERCISE"
        ? getExercise(homeworkId).then(setExercise)
        : getLearning().then((data) => {
            const found = data.homework.find(
              (h) => h.id === homeworkId && h.format === "MANUAL",
            );
            if (!found) {
              setLoadError(t("learning.readPage.notFound"));
              return;
            }
            setItem(found);
          });

    load
      .catch(() => setLoadError(t("learning.readPage.loadError")))
      .finally(() => setLoading(false));
  }, [homeworkId, format]);

  const title = exercise?.title ?? item?.title ?? "";
  const passage = exercise?.instructions ?? item?.instructions ?? "";
  const ready = exercise !== null || item !== null;

  return (
    <div className="mx-auto max-w-2xl px-6 py-12">
      <Link
        to="/aprendizaje"
        className="mb-8 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        {t("learning.readPage.back")}
      </Link>

      {loading && (
        <p className="mt-6 animate-pulse text-sm text-muted-foreground">
          {t("learning.readPage.loading")}
        </p>
      )}

      {loadError && !ready && (
        <p className="mt-6 text-sm text-destructive">{loadError}</p>
      )}

      {ready && (
        <>
          <h1 className="font-display text-3xl font-semibold text-primary">
            {title}
          </h1>

          <div className="mt-4 whitespace-pre-wrap rounded-lg border bg-card p-5 leading-relaxed text-foreground">
            {passage}
          </div>

          {exercise ? (
            <ExerciseForm exercise={exercise} />
          ) : item ? (
            <ManualAnswerForm
              homeworkId={homeworkId}
              initialResponse={item.response}
              readOnly={item.status === "REVIEWED"}
              labels={{
                yourAnswer: t("learning.readPage.yourAnswer"),
                placeholder: t("learning.readPage.placeholder"),
                submit: t("learning.readPage.submit"),
                submitting: t("learning.readPage.submitting"),
                autosaveHint: t("learning.writePage.autosaveHint"),
              }}
              onSubmitted={() => navigate({ to: "/aprendizaje" })}
            />
          ) : null}
        </>
      )}
    </div>
  );
}
