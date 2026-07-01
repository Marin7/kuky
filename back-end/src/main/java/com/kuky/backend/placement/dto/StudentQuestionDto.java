package com.kuky.backend.placement.dto;

import java.util.List;
import java.util.UUID;

/** Student-facing question — the answer key (isCorrect / cefrLevel) is intentionally absent. */
public record StudentQuestionDto(
        UUID id,
        String kind,
        String prompt,
        String audioUrl,
        UUID audioFileId,
        List<StudentOptionDto> options
) {}
