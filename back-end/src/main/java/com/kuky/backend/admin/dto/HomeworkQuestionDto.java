package com.kuky.backend.admin.dto;

import java.util.List;
import java.util.UUID;

/**
 * Teacher-facing question shape — includes the answer key ({@code correct} flags
 * and, for fill-blank, the accepted answers as options). Never sent to students.
 */
public record HomeworkQuestionDto(
        UUID id,            // null on create
        String kind,        // SINGLE_CHOICE | MULTI_CHOICE | FILL_BLANK
        String prompt,
        List<OptionDto> options
) {
    public record OptionDto(
            UUID id,        // null on create
            String label,
            boolean correct
    ) {}
}
