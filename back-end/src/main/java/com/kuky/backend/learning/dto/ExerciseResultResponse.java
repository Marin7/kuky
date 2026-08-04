package com.kuky.backend.learning.dto;

import java.util.List;
import java.util.UUID;

/**
 * The graded outcome of a self-correcting exercise — overall score plus
 * per-question feedback (including the correct answers, revealed post-submit).
 */
public record ExerciseResultResponse(
        int scorePercent,            // 0–100, rounded
        int fullyCorrectCount,
        int totalQuestions,
        List<QuestionResultDto> questions
) {
    public record QuestionResultDto(
            UUID questionId,
            double score,                 // 0..1 (fractional for MULTI_CHOICE / multi-unit)
            boolean correct,              // score == 1
            List<UUID> correctOptionIds,  // for choice — revealed post-submit
            List<String> acceptedAnswers, // unused (kept for API shape); structured kinds use unitResults
            List<UnitResultDto> unitResults, // structured multi-unit kinds; empty for choice
            List<UUID> selectedOptionIds, // student's choice picks (empty otherwise)
            String answerText             // unused for current kinds; null
    ) {}

    public record UnitResultDto(
            int index,
            double score,
            boolean correct,
            String studentDisplay,
            List<String> expectedDisplay
    ) {}
}
