package com.kuky.backend.placement.dto;

import java.util.List;
import java.util.UUID;

public record UpsertQuestionRequest(
        String skill,
        String kind,
        String prompt,
        String audioUrl,
        UUID audioFileId,
        Boolean active,
        List<OptionInput> options
) {
    public record OptionInput(String label, boolean isCorrect) {}
}
