package com.kuky.backend.learning.dto;

import java.util.List;
import java.util.UUID;

/**
 * A self-correcting exercise as seen by a student. When {@code status == GRADED}
 * the {@code result} is populated so the locked exercise re-renders with feedback.
 */
public record ExerciseResponse(
        UUID id,
        String title,
        String instructions,
        String format,                       // always EXERCISE
        String status,                       // PENDING (not taken) or GRADED (locked)
        List<ExerciseQuestionDto> questions,
        ExerciseResultResponse result        // null unless GRADED
) {}
