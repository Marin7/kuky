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
import { AudioPlayer } from "./AudioPlayer";

interface Props {
  homeworkId: string;
  format: HomeworkFormat;
}

/**
 * Listening ("Escucha") homework on its own page: the audio source plays
 * prominently on top, with the questions (EXERCISE) or a free-text answer
 * (MANUAL) below — mirrors the reading page.
 */
export function HomeworkListeningPage({ homeworkId, format }: Props) {
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
              setLoadError(t("learning.listenPage.notFound"));
              return;
            }
            setItem(found);
          });

    load
      .catch(() => setLoadError(t("learning.listenPage.loadError")))
      .finally(() => setLoading(false));
  }, [homeworkId, format]);

  const title = exercise?.title ?? item?.title ?? "";
  const instructions = exercise?.instructions ?? item?.instructions ?? "";
  const audioUrl = exercise?.audioUrl ?? item?.audioUrl ?? null;
  const audioFileId = exercise?.audioFileId ?? item?.audioFileId ?? null;
  const ready = exercise !== null || item !== null;

  return (
    <div className="mx-auto max-w-2xl px-6 py-12">
      <Link
        to="/aprendizaje"
        className="mb-8 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        {t("learning.listenPage.back")}
      </Link>

      {loading && (
        <p className="mt-6 animate-pulse text-sm text-muted-foreground">
          {t("learning.listenPage.loading")}
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

          {(audioUrl || audioFileId) && (
            <div className="mt-4">
              <AudioPlayer audioUrl={audioUrl} audioFileId={audioFileId} />
            </div>
          )}

          {instructions && (
            <p className="mt-4 whitespace-pre-wrap leading-relaxed text-muted-foreground">
              {instructions}
            </p>
          )}

          {exercise ? (
            <ExerciseForm exercise={exercise} />
          ) : item ? (
            <ManualAnswerForm
              homeworkId={homeworkId}
              initialResponse={item.response}
              readOnly={item.status === "REVIEWED"}
              labels={{
                yourAnswer: t("learning.listenPage.yourAnswer"),
                placeholder: t("learning.listenPage.placeholder"),
                submit: t("learning.listenPage.submit"),
                submitting: t("learning.listenPage.submitting"),
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
