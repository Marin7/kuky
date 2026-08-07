import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getExercise,
  type ExerciseResponse,
  type HomeworkItem,
} from "@/lib/learning";
import { ExerciseForm } from "./ExerciseForm";
import { ManualAnswerForm } from "./ManualAnswerForm";
import { AudioPlayer } from "./AudioPlayer";

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
  const [exercise, setExercise] = useState<ExerciseResponse | null>(null);
  const [loading, setLoading] = useState(item.format === "EXERCISE");
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    if (item.format !== "EXERCISE") {
      setLoading(false);
      return;
    }
    let cancelled = false;
    if (!exercise || exercise.id !== item.id) {
      setLoading(true);
    }
    setLoadError(null);
    getExercise(item.id)
      .then((data) => {
        if (!cancelled) setExercise(data);
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
  }, [item.id, item.format, item.status, t]);

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

  if (item.format === "EXERCISE" && exercise) {
    const audioUrl = exercise.audioUrl;
    const audioFileId = exercise.audioFileId;
    return (
      <div className="space-y-3">
        {(audioUrl || audioFileId) && (
          <AudioPlayer audioUrl={audioUrl} audioFileId={audioFileId} />
        )}
        {exercise.instructions && (
          <div className="whitespace-pre-wrap rounded-lg border bg-card p-4 text-base leading-relaxed text-foreground">
            {exercise.instructions}
          </div>
        )}
        <ExerciseForm exercise={exercise} onGraded={onChanged} />
      </div>
    );
  }

  // MANUAL
  const audioUrl = item.audioUrl;
  const audioFileId = item.audioFileId;
  const showPassageBox = item.homeworkType === "READ";

  return (
    <div className="space-y-3">
      {(audioUrl || audioFileId) && (
        <AudioPlayer audioUrl={audioUrl} audioFileId={audioFileId} />
      )}
      {item.instructions &&
        (showPassageBox ? (
          <div className="whitespace-pre-wrap rounded-lg border bg-card p-4 text-base leading-relaxed text-foreground">
            {item.instructions}
          </div>
        ) : (
          <p className="whitespace-pre-wrap text-sm leading-relaxed text-muted-foreground">
            {item.instructions}
          </p>
        ))}
      <ManualAnswerForm
        homeworkId={item.id}
        initialResponse={item.response}
        readOnly={item.status === "REVIEWED"}
        labels={{
          yourAnswer: t("learning.submitDialog.yourAnswer"),
          placeholder: t("learning.submitDialog.placeholder"),
          submit: t("learning.submitDialog.submit"),
          submitting: t("learning.submitDialog.submitting"),
          autosaveHint: t("learning.writePage.autosaveHint"),
        }}
        onSubmitted={onChanged}
      />
    </div>
  );
}
