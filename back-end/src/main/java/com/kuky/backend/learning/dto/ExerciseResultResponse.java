package com.kuky.backend.learning.dto;

import java.util.List;
import java.util.UUID;

/**
 * The graded outcome of a self-correcting exercise — overall score plus
 * per-question feedback (including the correct answers, revealed post-submit).
 * The three summary fields are null for unit page activities, which show students
 * per-answer marks only (no score).
 */
public record ExerciseResultResponse(
        Integer scorePercent,        // 0–100, rounded; null for page activities
        Integer fullyCorrectCount,
        Integer totalQuestions,
        List<QuestionResultDto> questions
) {
    /** The per-question marks without the overall score (what a page activity shows a student). */
    public ExerciseResultResponse withoutScore() {
        return new ExerciseResultResponse(null, null, null, questions);
    }

    public record QuestionResultDto(
            UUID questionId,
            double score,                 // 0..1 (fractional for MULTI_CHOICE / multi-unit)
            boolean correct,              // score == 1
            List<UUID> correctOptionIds,  // for choice — revealed post-submit
            List<String> acceptedAnswers, // unused (kept for API shape); structured kinds use unitResults
            List<UnitResultDto> unitResults, // structured multi-unit kinds; empty for choice
            List<UUID> selectedOptionIds  // student's choice picks (empty otherwise)
    ) {}

    public record UnitResultDto(
            int index,
            double score,
            boolean correct,
            String studentDisplay,
            List<String> expectedDisplay,
            String label // identifies the unit when its position isn't self-explanatory (MATCHING left item)
    ) {
        public UnitResultDto(int index, double score, boolean correct,
                            String studentDisplay, List<String> expectedDisplay) {
            this(index, score, correct, studentDisplay, expectedDisplay, null);
        }
    }
}
