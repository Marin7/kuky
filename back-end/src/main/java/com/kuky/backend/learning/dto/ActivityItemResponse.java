package com.kuky.backend.learning.dto;

import com.kuky.backend.learning.model.FormattedTextSegment;

import java.util.List;
import java.util.UUID;

/**
 * Student-facing activity detail (MANUAL or EXERCISE).
 * Placement: insert after {@code triggerPage} of {@code triggerFileId}.
 * Media: {@code instructionsText} + optional {@code youtubeUrl}.
 */
public record ActivityItemResponse(
        UUID id,
        String title,
        String format,
        String status,
        String level,
        String homeworkType,
        UUID triggerFileId,
        Integer triggerPage,
        String instructionsText,
        String youtubeUrl,
        UUID imageId,
        String reviewModel,
        List<FormattedTextSegment> response,
        List<FormattedTextSegment> feedback,
        String feedbackText,
        Integer scorePercent,
        List<ExerciseQuestionDto> questions,
        ExerciseResultResponse result,
        String teacherFeedback,
        List<ManualAnswerViewDto> answers
) {}
