package com.kuky.backend.admin.dto;

import com.kuky.backend.learning.dto.ExerciseQuestionDto;
import com.kuky.backend.learning.dto.ExerciseResultResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Admin "Errores frecuentes" view: a student's wrong answers on auto-graded
 * homework questions, newest first, one page at a time.
 * {@code summary} and {@code entries} honour the period/label filters;
 * {@code allTimeMistakeCount} and {@code labelOptions} do not.
 */
public record StudentMistakesResponse(
        int allTimeMistakeCount,
        Summary summary,
        List<LabelOption> labelOptions,
        String appliedPeriod,
        String appliedLabel,
        int page,
        int pageSize,
        int totalPages,
        int totalEntries,
        List<Entry> entries
) {
    /** Counts per auto-graded contribution (a numbered item counts on its own). */
    public record Summary(int mistakeCount, int answeredCount) {}

    /** {@code key} is the case-insensitive group key; {@code label} the first spelling seen. */
    public record LabelOption(String key, String label) {}

    /** One auto-graded question with at least one wrong contribution, as submitted. */
    public record Entry(
            UUID submissionId,
            UUID assignmentId,
            String homeworkTitle,
            String homeworkFormat,
            List<String> labels,
            Instant submittedAt,
            int questionPosition,
            int mistakeCount,
            ExerciseQuestionDto question,
            ExerciseResultResponse.QuestionResultDto result
    ) {}
}
