package com.kuky.backend.learning.dto;

import java.util.List;
import java.util.UUID;

/**
 * Student-facing question — the answer key is intentionally absent. Choice
 * questions carry their selectable options (without a correct flag); fill-blank
 * questions carry an empty options list (the student types a free answer).
 */
public record ExerciseQuestionDto(
        UUID id,
        String kind,
        String prompt,
        List<StudentOptionDto> options
) {
    public record StudentOptionDto(UUID id, String label) {}
}
