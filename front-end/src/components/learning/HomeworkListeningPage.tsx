import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "@tanstack/react-router";
import {
  getExercise,
  getLearning,
  type ExerciseResponse,
  type HomeworkItem,
  type HomeworkFormat,
} from "@/lib/learning";
import { ExerciseForm } from "./ExerciseForm";
import { ManualMultiAnswerForm } from "./ManualMultiAnswerForm";
import { AudioPlayer } from "./AudioPlayer";
import { RichTextViewer } from "./richtext/RichTextViewer";

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
    <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 sm:py-8">
      <Link
        to="/aprendizaje"
        className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        {t("learning.listenPage.back")}
      </Link>

      {loading && (
        <p className="mt-4 animate-pulse text-sm text-muted-foreground">
          {t("learning.listenPage.loading")}
        </p>
      )}

      {loadError && !ready && (
        <p className="mt-4 text-sm text-destructive">{loadError}</p>
      )}

      {ready && (
        <>
          <h1 className="font-display text-2xl font-semibold text-primary sm:text-3xl">
            {title}
          </h1>

          {instructions && (
            <p className="mt-3 whitespace-pre-wrap text-base leading-relaxed text-muted-foreground">
              {instructions}
            </p>
          )}

          {(audioUrl || audioFileId) && (
            <div className="mt-3">
              <AudioPlayer audioUrl={audioUrl} audioFileId={audioFileId} />
            </div>
          )}

          {exercise ? (
            <ExerciseForm exercise={exercise} />
          ) : item ? (
            <>
              <ManualMultiAnswerForm
                homeworkId={homeworkId}
                questions={(item.questions ?? []).map((q) => ({
                  id: q.id,
                  prompt: q.prompt,
                }))}
                initialAnswers={item.answers}
                readOnly={item.status === "REVIEWED"}
              />
              {item.feedbackText ? (
                <div className="mt-6 space-y-2">
                  <p className="text-base font-medium text-foreground">
                    {t("learning.writePage.teacherFeedback")}
                  </p>
                  <div className="rounded-md border bg-muted/20 p-3 text-sm whitespace-pre-wrap break-all [overflow-wrap:anywhere]">
                    {item.feedbackText}
                  </div>
                </div>
              ) : item.feedback && item.feedback.length > 0 ? (
                <div className="mt-6 space-y-2">
                  <p className="text-base font-medium text-foreground">
                    {t("learning.writePage.teacherFeedback")}
                  </p>
                  <div className="rounded-md border bg-muted/20 p-3">
                    <RichTextViewer segments={item.feedback} />
                  </div>
                </div>
              ) : null}
            </>
          ) : null}
        </>
      )}
    </div>
  );
}
