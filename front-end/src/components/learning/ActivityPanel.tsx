import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getActivity,
  resolveComposition,
  isStudentHomeworkEditable,
  submitActivity,
  submitActivityAnswers,
  submitActivityAnswersItem,
  type ActivityItem,
  type ExerciseResponse,
} from "@/lib/learning";
import { ManualMultiAnswerForm } from "./ManualMultiAnswerForm";
import { ExerciseForm } from "./ExerciseForm";
import { MixedHomeworkForm } from "./MixedHomeworkForm";
import { RichTextViewer } from "./richtext/RichTextViewer";
import { TextWithLinks } from "./TextWithLinks";

interface Props {
  activityId: string;
  onChanged?: () => void;
}

function toExerciseResponse(
  item: ActivityItem,
  title: string,
): ExerciseResponse {
  return {
    id: item.id,
    title,
    instructions: "",
    format: item.format === "MIXED" ? "MIXED" : "EXERCISE",
    composition: item.composition,
    status: item.status,
    homeworkType: null,
    audioUrl: null,
    audioFileId: null,
    mediaSourceKind: null,
    questions: item.questions ?? [],
    result: item.result,
    answers: item.answers,
    scorePercent: null,
    feedbackText: item.feedbackText,
    teacherFeedback: item.teacherFeedback,
  };
}

/** A page activity's work, shown in the unit PDF viewer right under its page. */
export function ActivityPanel({ activityId, onChanged }: Props) {
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

  const composition = resolveComposition(item);
  const title =
    item.title ?? t("learning.activities.pageTitle", { page: item.page });
  return (
    <div className="space-y-4">
      {composition === "MIXED" ? (
        <MixedHomeworkForm
          assignment={{
            id: item.id,
            status: item.status,
            questions: item.questions ?? [],
            result: item.result,
            answers: item.answers,
            feedbackText: item.feedbackText,
            teacherFeedback: item.teacherFeedback,
          }}
          // Activities carry no score: no combined % or auto-results summary.
          hideCombinedScore
          hideAutoResultsSummary
          submitAnswers={submitActivityAnswersItem}
          onSubmitted={handleChanged}
        />
      ) : composition === "ALL_AUTO" ? (
        <ExerciseForm
          exercise={toExerciseResponse(item, title)}
          onGraded={handleChanged}
          submitAnswers={submitActivityAnswers}
        />
      ) : (
        <>
          {item.feedbackText ? (
            <div className="space-y-1">
              <p className="text-sm font-medium">
                {t("learning.writePage.teacherFeedback")}
              </p>
              <div className="rounded-md border bg-muted/20 p-3 text-sm whitespace-pre-wrap break-all [overflow-wrap:anywhere]">
                <TextWithLinks text={item.feedbackText} />
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
          <ManualMultiAnswerForm
            key={`${item.id}-${item.status}`}
            homeworkId={item.id}
            questions={(item.questions ?? []).map((q) => ({
              id: q.id,
              prompt: q.prompt,
            }))}
            initialAnswers={item.answers}
            readOnly={!isStudentHomeworkEditable(item.status)}
            submitAnswer={(id, _response, answers) =>
              submitActivity(id, undefined, answers)
            }
            onSubmitted={handleChanged}
          />
        </>
      )}
    </div>
  );
}
