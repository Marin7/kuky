package com.kuky.backend.learning.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.kuky.backend.learning.dto.ExerciseQuestionDto;
import com.kuky.backend.learning.dto.ExerciseResultResponse;
import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.dto.ManualAnswerViewDto;
import com.kuky.backend.learning.dto.UnitRef;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkAnswer;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkComposition;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.model.HomeworkType;
import com.kuky.backend.learning.model.QuestionKind;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Builds a {@link HomeworkItemResponse} from a (shared) assignment definition and
 * the calling student's (optional) submission, deriving effective status and the
 * overdue flag. Shared by {@code LearningService} and {@code HomeworkSubmissionService}.
 */
final class HomeworkItems {

    private HomeworkItems() {}

    static HomeworkItemResponse toResponse(HomeworkAssignment a, HomeworkSubmission submission, LocalDate today) {
        return toResponse(a, submission, today, null, null, null, List.of(), List.of(), null, null, false);
    }

    static HomeworkItemResponse toResponse(HomeworkAssignment a, HomeworkSubmission submission,
                                           LocalDate today, UnitRef unit, Integer unitPosition) {
        return toResponse(a, submission, today, null, unit, unitPosition, List.of(), List.of(), null, null, false);
    }

    static HomeworkItemResponse toResponse(HomeworkAssignment a, HomeworkSubmission submission,
                                           LocalDate today, UnitRef unit, Integer unitPosition,
                                           List<HomeworkQuestion> questions,
                                           List<HomeworkAnswer> answers,
                                           List<ExerciseQuestionDto> studentQuestions,
                                           ExerciseResultResponse result) {
        return toResponse(a, submission, today, null, unit, unitPosition, questions, answers,
                studentQuestions, result, false);
    }

    static HomeworkItemResponse toResponse(HomeworkAssignment a, HomeworkSubmission submission,
                                           LocalDate today, LocalDate dueOn, UnitRef unit, Integer unitPosition,
                                           List<HomeworkQuestion> questions,
                                           List<HomeworkAnswer> answers,
                                           List<ExerciseQuestionDto> studentQuestions,
                                           ExerciseResultResponse result,
                                           boolean unseen) {
        HomeworkComposition composition = HomeworkCompositionSupport.compositionFromQuestions(
                a.getHomeworkType(), questions == null ? List.of() : questions.stream()
                        .map(q -> (HomeworkCompositionSupport.HasKind) q::getKind).toList());
        // Prefer derived from loaded questions; fall back to stored format when questions not loaded (WRITE / list).
        if ((questions == null || questions.isEmpty()) && a.getHomeworkType() != HomeworkType.WRITE) {
            composition = compositionFromFormat(a);
        } else if (a.getHomeworkType() == HomeworkType.WRITE) {
            composition = HomeworkComposition.WRITE;
        }

        String status = submission != null ? submission.getStatus() : HomeworkStatus.PENDING.name();
        boolean write = composition == HomeworkComposition.WRITE;
        List<FormattedTextSegment> response = write
                ? (submission != null ? FormattedTextSegment.fromJson(submission.getResponseText()) : null)
                : null;
        boolean annotated = submission != null && "ANNOTATED".equals(submission.getReviewModel());
        List<FormattedTextSegment> feedback = submission != null && !annotated
                ? FormattedTextSegment.fromJson(submission.getFeedback()) : null;
        String feedbackText = annotated
                ? FormattedTextSegment.decodePlainFeedback(submission.getFeedback()) : null;
        boolean overdue = HomeworkDueDates.overdue(dueOn, today, status);
        String type = a.getHomeworkType() == null ? null : a.getHomeworkType().name();
        String level = a.getLevel() == null ? null : a.getLevel().name();
        String format = a.getFormat() == null ? HomeworkFormat.MANUAL.name() : a.getFormat().name();
        Integer scorePercent = submission != null ? submission.getScorePercent() : null;
        Integer provisionalScorePercent = null;
        if (composition == HomeworkComposition.MIXED
                && submission != null
                && HomeworkStatus.SUBMITTED.name().equals(status)
                && result != null) {
            provisionalScorePercent = result.scorePercent();
            scorePercent = null;
        }
        boolean hasTeacherFeedback = submission != null
                && FormattedTextSegment.hasTeacherFeedback(submission.getFeedback());
        Instant contentRevisedAt = HomeworkStatus.PENDING.name().equals(status)
                ? a.getContentRevisedAt() : null;

        List<ExerciseQuestionDto> questionDtos = studentQuestions != null
                ? studentQuestions
                : (composition == HomeworkComposition.ALL_MANUAL || composition == HomeworkComposition.MIXED
                || composition == HomeworkComposition.ALL_AUTO
                ? (questions == null ? List.of() : questions.stream().map(HomeworkItems::toQuestionDto).toList())
                : List.of());

        List<ManualAnswerViewDto> answerViews = List.of();
        if (answers != null && !answers.isEmpty()
                && (composition == HomeworkComposition.ALL_MANUAL || composition == HomeworkComposition.MIXED)) {
            boolean stripTeacherScores = HomeworkStatus.SUBMITTED.name().equals(status);
            answerViews = answers.stream()
                    .filter(ans -> ans.getPromptSnapshot() != null || ans.getAnswerText() != null)
                    .map(ans -> toAnswerView(ans, stripTeacherScores))
                    .toList();
        }

        return new HomeworkItemResponse(
                a.getId(),
                a.getTitle(),
                a.getInstructions(),
                dueOn,
                type,
                level,
                format,
                composition.name(),
                status,
                submission != null ? submission.getReviewModel() : null,
                response,
                feedback,
                feedbackText,
                scorePercent,
                provisionalScorePercent,
                submission != null ? submission.getSubmittedAt() : null,
                overdue,
                a.getAudioUrl(),
                a.getAudioFileId(),
                a.getMediaSourceKind() == null ? null : a.getMediaSourceKind().name(),
                unit,
                unitPosition,
                hasTeacherFeedback,
                unseen,
                questionDtos,
                answerViews,
                result,
                contentRevisedAt
        );
    }

    /** Legacy helper name — true for non-WRITE question-based manual (not auto-only). */
    static boolean isMultiManual(HomeworkAssignment a) {
        return a.getFormat() == HomeworkFormat.MANUAL && a.getHomeworkType() != HomeworkType.WRITE;
    }

    static boolean hasQuestionAnswers(HomeworkAssignment a) {
        HomeworkComposition c = compositionFromFormat(a);
        return c == HomeworkComposition.ALL_MANUAL || c == HomeworkComposition.MIXED
                || c == HomeworkComposition.ALL_AUTO;
    }

    static HomeworkComposition compositionFromFormat(HomeworkAssignment a) {
        if (a.getHomeworkType() == HomeworkType.WRITE) return HomeworkComposition.WRITE;
        if (a.getFormat() == HomeworkFormat.MIXED) return HomeworkComposition.MIXED;
        if (a.getFormat() == HomeworkFormat.EXERCISE) return HomeworkComposition.ALL_AUTO;
        return HomeworkComposition.ALL_MANUAL;
    }

    private static ExerciseQuestionDto toQuestionDto(HomeworkQuestion q) {
        if (q.getKind() == QuestionKind.FREE_TEXT) {
            return new ExerciseQuestionDto(
                    q.getId(),
                    QuestionKind.FREE_TEXT.name(),
                    q.getPrompt(),
                    List.of(),
                    JsonNodeFactory.instance.objectNode());
        }
        // Without strip helper here — callers that need key-stripped structured questions
        // should pass studentQuestions from ExerciseGradingService.
        return new ExerciseQuestionDto(
                q.getId(),
                q.getKind().name(),
                q.getPrompt(),
                List.of(),
                JsonNodeFactory.instance.objectNode());
    }

    private static ManualAnswerViewDto toAnswerView(HomeworkAnswer a, boolean stripTeacherScores) {
        if (stripTeacherScores) {
            return ManualAnswerViewDto.fromStored(
                    a.getQuestionId(), a.getPromptSnapshot(), a.getAnswerText());
        }
        Double score = a.getScore() == null ? null : a.getScore().doubleValue();
        return ManualAnswerViewDto.fromStored(
                a.getQuestionId(), a.getPromptSnapshot(), a.getAnswerText(),
                a.getTeacherScorePercent(), score);
    }
}
