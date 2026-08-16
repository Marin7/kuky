import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "@tanstack/react-router";
import {
  getExercise,
  getLearning,
  resolveComposition,
  isAutoTakeComposition,
  isListeningMediaReady,
  isStudentHomeworkEditable,
  type ExerciseResponse,
  type HomeworkItem,
  type HomeworkFormat,
} from "@/lib/learning";
import { markHomeworkSeen, notifyBadgesChanged } from "@/lib/notifications";
import { ExerciseForm } from "./ExerciseForm";
import { MixedHomeworkForm } from "./MixedHomeworkForm";
import { ManualMultiAnswerForm } from "./ManualMultiAnswerForm";
import { AudioPlayer } from "./AudioPlayer";
import { RichTextViewer } from "./richtext/RichTextViewer";
import { HomeworkDueOn } from "./HomeworkDueOn";

interface Props {
  homeworkId: string;
  format: HomeworkFormat;
}

/**
 * Listening ("Escucha") homework on its own page: the audio source plays
 * prominently on top, with questions (auto / mixed / manual) below.
 */
export function HomeworkListeningPage({ homeworkId, format }: Props) {
  const { t } = useTranslation();

  const [exercise, setExercise] = useState<ExerciseResponse | null>(null);
  const [item, setItem] = useState<HomeworkItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    const compositionHint = resolveComposition({ format });
    const load = isAutoTakeComposition(compositionHint)
      ? getExercise(homeworkId).then((data) => {
          setExercise(data);
          notifyBadgesChanged();
        })
      : getLearning().then((data) => {
          const found = data.homework.find((h) => h.id === homeworkId);
          if (!found) {
            setLoadError(t("learning.listenPage.notFound"));
            return;
          }
          if (!isListeningMediaReady(found)) {
            setLoadError(t("learning.listenPage.mediaIncomplete"));
            return;
          }
          setItem(found);
          return markHomeworkSeen(homeworkId).then(() => notifyBadgesChanged());
        });

    load
      .catch(() => setLoadError(t("learning.listenPage.loadError")))
      .finally(() => setLoading(false));
  }, [homeworkId, format, t]);

  const title = exercise?.title ?? item?.title ?? "";
  const instructions = exercise?.instructions ?? item?.instructions ?? "";
  const audioUrl = exercise?.audioUrl ?? item?.audioUrl ?? null;
  const audioFileId = exercise?.audioFileId ?? item?.audioFileId ?? null;
  const mediaSourceKind =
    exercise?.mediaSourceKind ?? item?.mediaSourceKind ?? null;
  const ready = exercise !== null || item !== null;
  const composition = exercise
    ? resolveComposition(exercise)
    : item
      ? resolveComposition(item)
      : resolveComposition({ format });

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
          <HomeworkDueOn
            dueOn={item?.dueOn ?? exercise?.dueOn}
            className="mt-2 block text-sm text-muted-foreground"
          />

          {instructions && (
            <p className="mt-3 whitespace-pre-wrap text-base leading-relaxed text-muted-foreground">
              {instructions}
            </p>
          )}

          {(audioUrl || audioFileId) && (
            <div className="mt-3">
              <AudioPlayer
                mediaSourceKind={mediaSourceKind}
                audioUrl={audioUrl}
                audioFileId={audioFileId}
              />
            </div>
          )}

          {composition === "MIXED" && exercise ? (
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
              }}
              allowEmojiInsert
              onHomeworkUpdated={() =>
                getExercise(homeworkId)
                  .then(setExercise)
                  .catch(() => {})
              }
            />
          ) : composition === "ALL_AUTO" && exercise ? (
            <ExerciseForm
              exercise={exercise}
              onHomeworkUpdated={() =>
                getExercise(homeworkId)
                  .then(setExercise)
                  .catch(() => {})
              }
            />
          ) : item ? (
            <>
              <ManualMultiAnswerForm
                homeworkId={homeworkId}
                questions={(item.questions ?? []).map((q) => ({
                  id: q.id,
                  prompt: q.prompt,
                }))}
                initialAnswers={item.answers}
                readOnly={!isStudentHomeworkEditable(item.status)}
                contentRevisedAt={item.contentRevisedAt}
                onHomeworkUpdated={() =>
                  getLearning()
                    .then((data) => {
                      const found = data.homework.find(
                        (h) => h.id === homeworkId,
                      );
                      if (found) setItem(found);
                    })
                    .catch(() => {})
                }
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
