package com.kuky.backend.learning.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

/**
 * Student-facing question — the answer key is intentionally absent. Choice
 * questions carry their selectable options (without a correct flag).
 * Structured kinds expose a stripped {@code structure} (no accepted answers / pairs)
 * and an empty options list.
 */
public record ExerciseQuestionDto(
        UUID id,
        String kind,
        String prompt,
        List<StudentOptionDto> options,
        JsonNode structure
) {
    public record StudentOptionDto(UUID id, String label) {}
}
