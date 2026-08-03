package com.kuky.backend.admin.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

/**
 * Teacher-facing question shape — includes the answer key ({@code correct} flags
 * and, for fill-blank, the accepted answers as options). Never sent to students.
 * Structured kinds carry their payload in {@code structure} and use an empty options list.
 */
public record HomeworkQuestionDto(
        UUID id,            // null on create
        String kind,
        String prompt,
        List<OptionDto> options,
        JsonNode structure  // null/empty for legacy kinds
) {
    public record OptionDto(
            UUID id,        // null on create
            String label,
            boolean correct
    ) {}
}
