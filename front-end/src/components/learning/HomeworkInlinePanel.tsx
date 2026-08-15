import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getExercise,
  resolveComposition,
  isAutoTakeComposition,
  isStudentHomeworkEditable,
  pinInstructionsAboveWordBank,
  type ExerciseResponse,
  type HomeworkItem,
} from "@/lib/learning";
import { markHomeworkSeen, notifyBadgesChanged } from "@/lib/notifications";
import { ExerciseForm } from "./ExerciseForm";
import { MixedHomeworkForm } from "./MixedHomeworkForm";
import { ManualAnswerForm } from "./ManualAnswerForm";
import { ManualMultiAnswerForm } from "./ManualMultiAnswerForm";
import { AudioPlayer } from "./AudioPlayer";
import { RichTextViewer } from "./richtext/RichTextViewer";

interface Props {
  item: HomeworkItem;
  /** Refresh parent list after submit / grade. */
  onChanged: () => void;
}

/**
 * Homework body for the unit expand/collapse view: loads exercise data when
 * needed and renders take / result / manual answer inline (no page navigate).
 */
export function HomeworkInlinePanel({ item, onChanged }: Props) {
  const { t } = useTranslation();
  const composition = resolveComposition(item);
  const needsFetch = isAutoTakeComposition(composition);
  const [exercise, setExercise] = useState<ExerciseResponse | null>(null);
  const [loading, setLoading] = useState(needsFetch);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    if (!needsFetch) {
      setLoading(false);
      if (item.unseen) {
        markHomeworkSeen(item.id)
          .then(() => {
            notifyBadgesChanged();
            onChanged();
          })
          .catch(() => {});
      }
      return;
    }
    let cancelled = false;
    if (!exercise || exercise.id !== item.id) {
      setLoading(true);
    }
    setLoadError(null);
    getExercise(item.id)
      .then((data) => {
        if (cancelled) return;
        setExercise(data);
        if (item.unseen) {
          notifyBadgesChanged();
          onChanged();
        }
      })
      .catch(() => {
        if (!cancelled) setLoadError(t("learning.units.inlineLoadError"));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
    // Re-fetch when homework status changes (e.g. after grade refresh).
    // eslint-disable-next-line react-hooks/exhaustive-deps -- exercise kept for silent refresh
  }, [item.id, composition, item.status, item.unseen, t]);

  if (loading) {
    return (
      <p className="animate-pulse text-sm text-muted-foreground">
        {t("learning.units.inlineLoading")}
      </p>
    );
  }

  if (loadError) {
    return <p className="text-sm text-destructive">{loadError}</p>;
  }

  if (composition === "MIXED" && exercise) {
    const audioUrl = exercise.audioUrl;
    const audioFileId = exercise.audioFileId;
    const pinIntro = pinInstructionsAboveWordBank(
      exercise.questions,
      exercise.homeworkType,
      exercise.status,
    );
    return (
      <div className="space-y-3">
        {exercise.instructions && !pinIntro && (
          <p className="whitespace-pre-wrap text-base leading-relaxed text-foreground">
            {exercise.instructions}
          </p>
        )}
        {(audioUrl || audioFileId) && (
          <AudioPlayer
            mediaSourceKind={exercise.mediaSourceKind}
            audioUrl={audioUrl}
            audioFileId={audioFileId}
          />
        )}
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
          onSubmitted={onChanged}
          onHomeworkUpdated={onChanged}
        />
      </div>
    );
  }

  if (composition === "ALL_AUTO" && exercise) {
    const audioUrl = exercise.audioUrl;
    const audioFileId = exercise.audioFileId;
    const pinIntro = pinInstructionsAboveWordBank(
      exercise.questions,
      exercise.homeworkType,
      exercise.status,
    );
    return (
      <div className="space-y-3">
        {exercise.instructions && !pinIntro && (
          <p className="whitespace-pre-wrap text-base leading-relaxed text-foreground">
            {exercise.instructions}
          </p>
        )}
        {(audioUrl || audioFileId) && (
          <AudioPlayer
            mediaSourceKind={exercise.mediaSourceKind}
            audioUrl={audioUrl}
            audioFileId={audioFileId}
          />
        )}
        <ExerciseForm
          exercise={exercise}
          onGraded={onChanged}
          onHomeworkUpdated={onChanged}
        />
      </div>
    );
  }

  // WRITE / ALL_MANUAL
  const audioUrl = item.audioUrl;
  const audioFileId = item.audioFileId;
  const showPassageBox = item.homeworkType === "READ";

  return (
    <div className="space-y-3">
      {item.instructions &&
        (showPassageBox ? (
          <div className="whitespace-pre-wrap rounded-lg border bg-card p-4 text-base leading-relaxed text-foreground">
            {item.instructions}
          </div>
        ) : (
          <p className="whitespace-pre-wrap text-base leading-relaxed text-foreground">
            {item.instructions}
          </p>
        ))}
      {(audioUrl || audioFileId) && (
        <AudioPlayer
          mediaSourceKind={item.mediaSourceKind}
          audioUrl={audioUrl}
          audioFileId={audioFileId}
        />
      )}
      {composition !== "WRITE" ? (
        <ManualMultiAnswerForm
          key={`${item.id}-${item.status}-${item.submittedAt ?? "draft"}`}
          homeworkId={item.id}
          questions={(item.questions ?? []).map((q) => ({
            id: q.id,
            prompt: q.prompt,
          }))}
          initialAnswers={item.answers}
          readOnly={!isStudentHomeworkEditable(item.status)}
          contentRevisedAt={item.contentRevisedAt}
          onSubmitted={onChanged}
          onHomeworkUpdated={onChanged}
        />
      ) : (
        <ManualAnswerForm
          homeworkId={item.id}
          initialResponse={item.response}
          readOnly={!isStudentHomeworkEditable(item.status)}
          contentRevisedAt={item.contentRevisedAt}
          onHomeworkUpdated={onChanged}
          labels={{
            yourAnswer: t("learning.submitDialog.yourAnswer"),
            placeholder: t("learning.submitDialog.placeholder"),
            submit: t("learning.submitDialog.submit"),
            submitting: t("learning.submitDialog.submitting"),
            autosaveHint: t("learning.writePage.autosaveHint"),
          }}
          onSubmitted={onChanged}
        />
      )}
      {item.feedbackText ? (
        <div className="space-y-1">
          <p className="text-sm font-medium">
            {t("learning.writePage.teacherFeedback")}
          </p>
          <div className="rounded-md border bg-muted/20 p-3 text-sm whitespace-pre-wrap break-all [overflow-wrap:anywhere]">
            {item.feedbackText}
          </div>
        </div>
      ) : item.feedback && item.feedback.length > 0 ? (
        <div className="space-y-1">
          <p className="text-sm font-medium">
            {t("learning.writePage.teacherFeedback")}
          </p>
          <div className="rounded-md border bg-muted/20 p-3">
            <RichTextViewer segments={item.feedback} />
          </div>
        </div>
      ) : null}
    </div>
  );
}
