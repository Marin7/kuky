import {
  getActivityExerciseSubmissionResult,
  saveActivityExerciseFeedback,
  type HomeworkFormat,
} from "@/lib/admin";
import { ExerciseResultDialog } from "@/components/admin/homework/ExerciseResultDialog";
import { ActivityReviewDialog } from "./ActivityReviewDialog";

interface Props {
  submissionId: string;
  /** EXERCISE (all auto-graded) shows the result; anything with free text opens the review. */
  format: HomeworkFormat;
  onClose: () => void;
  onChanged: () => void;
}

/** Opens a page-activity submission the way its kind needs (result view or teacher review). */
export function ActivitySubmissionDialog({
  submissionId,
  format,
  onClose,
  onChanged,
}: Props) {
  if (format === "EXERCISE") {
    return (
      <ExerciseResultDialog
        submissionId={submissionId}
        onClose={() => {
          onClose();
          onChanged();
        }}
        onFeedbackSaved={onChanged}
        load={getActivityExerciseSubmissionResult}
        saveFeedback={saveActivityExerciseFeedback}
      />
    );
  }
  return (
    <ActivityReviewDialog
      submissionId={submissionId}
      onClose={() => {
        onClose();
        onChanged();
      }}
      onReviewed={() => {
        onClose();
        onChanged();
      }}
    />
  );
}
