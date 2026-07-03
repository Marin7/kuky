package com.kuky.backend.learning.service;

import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.dto.UnitRef;
import com.kuky.backend.learning.model.FormattedTextSegment;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkFormat;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.HomeworkSubmission;

import java.time.LocalDate;
import java.util.List;

/**
 * Builds a {@link HomeworkItemResponse} from a (shared) assignment definition and
 * the calling student's (optional) submission, deriving effective status and the
 * overdue flag. Shared by {@code LearningService} and {@code HomeworkSubmissionService}.
 */
final class HomeworkItems {

    private HomeworkItems() {}

    /**
     * @param submission the student's submission, or {@code null} when none exists (⇒ PENDING)
     * @param today      the current date in the teacher's timezone, for overdue derivation
     */
    static HomeworkItemResponse toResponse(HomeworkAssignment a, HomeworkSubmission submission, LocalDate today) {
        return toResponse(a, submission, today, null);
    }

    /** Variant that attaches the owning unit, for the student's unit-grouped learning view. */
    static HomeworkItemResponse toResponse(HomeworkAssignment a, HomeworkSubmission submission,
                                           LocalDate today, UnitRef unit) {
        String status = submission != null ? submission.getStatus() : HomeworkStatus.PENDING.name();
        List<FormattedTextSegment> response =
                submission != null ? FormattedTextSegment.fromJson(submission.getResponseText()) : null;
        List<FormattedTextSegment> feedback =
                submission != null ? FormattedTextSegment.fromJson(submission.getFeedback()) : null;
        boolean overdue = a.getDueOn() != null
                && a.getDueOn().isBefore(today)
                && HomeworkStatus.PENDING.name().equals(status);
        String type = a.getHomeworkType() == null ? null : a.getHomeworkType().name();
        String level = a.getLevel() == null ? null : a.getLevel().name();
        String format = a.getFormat() == null ? HomeworkFormat.MANUAL.name() : a.getFormat().name();
        Integer scorePercent = submission != null ? submission.getScorePercent() : null;
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
                unit
        );
    }
}
