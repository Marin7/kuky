package com.kuky.backend.learning.dto;

import com.kuky.backend.learning.model.FormattedTextSegment;

import java.util.List;
import java.util.UUID;

/**
 * Student-facing activity detail (MANUAL, EXERCISE, or MIXED).
 * The instructions are page {@code page} of the unit PDF file {@code fileId}.
 * Carries no score: students see per-answer marks and teacher feedback only.
 */
public record ActivityItemResponse(
        UUID id,
        String title,
        String format,
        String composition,
        String status,
        UUID presentationId,
        UUID fileId,
        int page,
        UUID unitId,
        String reviewModel,
        List<FormattedTextSegment> response,
        List<FormattedTextSegment> feedback,
        String feedbackText,
        List<ExerciseQuestionDto> questions,
        ExerciseResultResponse result,
        String teacherFeedback,
        List<ManualAnswerViewDto> answers
) {}
