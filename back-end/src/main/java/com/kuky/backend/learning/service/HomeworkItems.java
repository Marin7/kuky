package com.kuky.backend.learning.service;

import com.kuky.backend.learning.dto.HomeworkItemResponse;
import com.kuky.backend.learning.model.HomeworkAssignment;
import com.kuky.backend.learning.model.HomeworkStatus;
import com.kuky.backend.learning.model.HomeworkSubmission;

import java.time.LocalDate;

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
        String status = submission != null ? submission.getStatus() : HomeworkStatus.PENDING.name();
        String response = submission != null ? submission.getResponseText() : null;
        boolean overdue = a.getDueOn() != null
                && a.getDueOn().isBefore(today)
                && HomeworkStatus.PENDING.name().equals(status);
        return new HomeworkItemResponse(
                a.getId(),
                a.getTitle(),
                a.getInstructions(),
                a.getDueOn(),
                status,
                response,
                submission != null ? submission.getSubmittedAt() : null,
                overdue
        );
    }
}
