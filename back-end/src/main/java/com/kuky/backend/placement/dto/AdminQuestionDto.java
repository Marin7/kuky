package com.kuky.backend.placement.dto;

import java.util.List;
import java.util.UUID;

/** Admin view of a question — includes the answer key (isCorrect). */
public record AdminQuestionDto(
        UUID id,
        String skill,
        int position,
        String kind,
        String prompt,
        String audioUrl,
        UUID audioFileId,
        boolean active,
        List<AdminOptionDto> options
) {
    public record AdminOptionDto(UUID id, String label, boolean isCorrect) {}
}
