package com.kuky.backend.learning.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.kuky.backend.learning.dto.ExerciseQuestionDto;
import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.dto.ManualAnswerViewDto;
import com.kuky.backend.learning.dto.UnitRef;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkAnswer;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkQuestion;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.HomeworkSubmission;
import com.kuky.backend.learning.model.HomeworkType;
import com.kuky.backend.learning.model.QuestionKind;

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
        return toResponse(a, submission, today, null, null, List.of(), List.of());
    }

    static HomeworkItemResponse toResponse(HomeworkAssignment a, HomeworkSubmission submission,
                                           LocalDate today, UnitRef unit, Integer unitPosition) {
        return toResponse(a, submission, today, unit, unitPosition, List.of(), List.of());
    }

    static HomeworkItemResponse toResponse(HomeworkAssignment a, HomeworkSubmission submission,
                                           LocalDate today, UnitRef unit, Integer unitPosition,
                                           List<HomeworkQuestion> freeTextQuestions,
                                           List<HomeworkAnswer> freeTextAnswers) {
        String status = submission != null ? submission.getStatus() : HomeworkStatus.PENDING.name();
        boolean multiManual = isMultiManual(a);
        List<FormattedTextSegment> response = multiManual
                ? null
                : (submission != null ? FormattedTextSegment.fromJson(submission.getResponseText()) : null);
        List<FormattedTextSegment> feedback =
                submission != null ? FormattedTextSegment.fromJson(submission.getFeedback()) : null;
        boolean overdue = a.getDueOn() != null
                && a.getDueOn().isBefore(today)
                && HomeworkStatus.PENDING.name().equals(status);
        String type = a.getHomeworkType() == null ? null : a.getHomeworkType().name();
        String level = a.getLevel() == null ? null : a.getLevel().name();
        String format = a.getFormat() == null ? HomeworkFormat.MANUAL.name() : a.getFormat().name();
        Integer scorePercent = submission != null ? submission.getScorePercent() : null;
        boolean hasTeacherFeedback = submission != null
                && FormattedTextSegment.hasTeacherFeedback(submission.getFeedback());

        List<ExerciseQuestionDto> questions = multiManual
                ? freeTextQuestions.stream().map(HomeworkItems::toFreeTextQuestionDto).toList()
                : List.of();
        List<ManualAnswerViewDto> answers = multiManual
                ? freeTextAnswers.stream().map(HomeworkItems::toAnswerView).toList()
                : List.of();

        return new HomeworkItemResponse(
                a.getId(),
                a.getTitle(),
                a.getInstructions(),
                a.getDueOn(),
                type,
                level,
                format,
                status,
                response,
                feedback,
                scorePercent,
                submission != null ? submission.getSubmittedAt() : null,
                overdue,
                a.getAudioUrl(),
                a.getAudioFileId(),
                unit,
                unitPosition,
                hasTeacherFeedback,
                questions,
                answers
        );
    }

    static boolean isMultiManual(HomeworkAssignment a) {
        return a.getFormat() == HomeworkFormat.MANUAL && a.getHomeworkType() != HomeworkType.WRITE;
    }

    private static ExerciseQuestionDto toFreeTextQuestionDto(HomeworkQuestion q) {
        return new ExerciseQuestionDto(
                q.getId(),
                QuestionKind.FREE_TEXT.name(),
                q.getPrompt(),
                List.of(),
                JsonNodeFactory.instance.objectNode());
    }

    private static ManualAnswerViewDto toAnswerView(HomeworkAnswer a) {
        return new ManualAnswerViewDto(a.getQuestionId(), a.getPromptSnapshot(), a.getAnswerText());
    }
}
