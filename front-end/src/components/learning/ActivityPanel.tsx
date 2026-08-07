import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getActivity,
  submitActivity,
  submitActivityAnswers,
  type ActivityItem,
  type ExerciseResponse,
} from "@/lib/learning";
import { extractYoutubeVideoId, youtubeEmbedUrl, activityImageUrl } from "@/lib/youtube";
import { ManualAnswerForm } from "./ManualAnswerForm";
import { ExerciseForm } from "./ExerciseForm";
import { RichTextViewer } from "./richtext/RichTextViewer";

interface Props {
  activityId: string;
  /** Compact layout when embedded in a presentation slot (media shown by parent). */
  compact?: boolean;
  onChanged?: () => void;
}

function toExerciseResponse(item: ActivityItem): ExerciseResponse {
  return {
    id: item.id,
    title: item.title,
    instructions: "",
    format: "EXERCISE",
    status: item.status,
    homeworkType: item.homeworkType,
    audioUrl: null,
    audioFileId: null,
    questions: item.questions ?? [],
    result: item.result,
    teacherFeedback: item.teacherFeedback,
  };
}

function ActivityMedia({ item }: { item: ActivityItem }) {
  const { t } = useTranslation();
  const videoId = extractYoutubeVideoId(item.youtubeUrl);
  if (!videoId && !item.imageId && !item.instructionsText?.trim()) return null;

  return (
    <div className="space-y-4">
      {videoId && (
        <div className="aspect-video w-full overflow-hidden rounded-md border bg-black">
          <iframe
            title={item.title}
            src={youtubeEmbedUrl(videoId)}
            className="h-full w-full"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
          />
        </div>
      )}
      {item.imageId && (
        <img
          src={activityImageUrl(item.imageId)}
          alt=""
          className="max-h-[28rem] w-full rounded-md border object-contain bg-muted/20"
        />
      )}
      {item.instructionsText?.trim() && (
        <div className="space-y-1">
          <p className="text-sm font-medium">
            {t("learning.activities.instructions")}
          </p>
          <p className="whitespace-pre-wrap text-sm leading-relaxed text-foreground">
            {item.instructionsText}
          </p>
        </div>
      )}
    </div>
  );
}

export function ActivityPanel({ activityId, compact, onChanged }: Props) {
  const { t } = useTranslation();
  const [item, setItem] = useState<ActivityItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = () => {
    setLoading(true);
    setError(null);
    getActivity(activityId)
      .then(setItem)
      .catch(() => setError(t("learning.activities.loadError")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps -- reload on id change
  }, [activityId]);

  const handleChanged = () => {
    reload();
    onChanged?.();
  };

  if (loading && !item) {
    return (
      <p className="animate-pulse text-sm text-muted-foreground">
        {t("learning.activities.loading")}
      </p>
    );
  }

  if (error && !item) {
    return <p className="text-sm text-destructive">{error}</p>;
  }

  if (!item) return null;

  return (
    <div className={compact ? "space-y-4" : "space-y-6"}>
      {!compact && (
        <h1 className="font-display text-2xl font-semibold text-primary sm:text-3xl">
          {item.title}
        </h1>
      )}

      <div className="flex flex-wrap items-center gap-2 text-sm">
        <span
          className={[
            "rounded-full px-2 py-0.5 text-xs font-medium",
            item.status === "PENDING"
              ? "bg-muted text-muted-foreground"
              : "bg-green-100 text-green-700",
          ].join(" ")}
        >
          {t(`learning.homework.status.${item.status}`)}
          {item.status === "GRADED" &&
            item.scorePercent != null &&
            ` — ${item.scorePercent}%`}
        </span>
      </div>

      {item.triggerPage != null && !compact && (
        <p className="text-sm text-muted-foreground">
          {t("learning.activities.insertAfterPage", {
            page: item.triggerPage,
          })}
        </p>
      )}

      {!compact && <ActivityMedia item={item} />}

      {item.format === "EXERCISE" ? (
        <ExerciseForm
          exercise={toExerciseResponse(item)}
          onGraded={handleChanged}
          submitAnswers={submitActivityAnswers}
        />
      ) : (
        <>
          {item.feedback && item.feedback.length > 0 && (
            <div className="space-y-1">
              <p className="text-sm font-medium">
                {t("learning.writePage.teacherFeedback")}
              </p>
              <div className="rounded-md border bg-muted/20 p-3">
                <RichTextViewer segments={item.feedback} />
              </div>
            </div>
          )}
          <ManualAnswerForm
            homeworkId={item.id}
            initialResponse={item.response}
            readOnly={item.status === "REVIEWED"}
            labels={{
              yourAnswer: t("learning.submitDialog.yourAnswer"),
              placeholder: t("learning.submitDialog.placeholder"),
              submit: t("learning.activities.submit"),
              submitting: t("learning.submitDialog.submitting"),
              autosaveHint: t("learning.writePage.autosaveHint"),
            }}
            submitAnswer={submitActivity}
            onSubmitted={handleChanged}
          />
        </>
      )}
    </div>
  );
}
