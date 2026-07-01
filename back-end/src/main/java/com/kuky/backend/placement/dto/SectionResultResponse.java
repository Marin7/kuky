package com.kuky.backend.placement.dto;

import java.util.List;
import java.util.UUID;

public record SectionResultResponse(
        String skill,
        int scorePercent,
        String cefrLevel, // "A0".."C2"
        List<QuestionResultDto> questionResults
) {
    public record QuestionResultDto(
            UUID questionId,
            double score,                 // 0..1 (fractional for MULTI_CHOICE)
            boolean correct,              // score == 1
            List<UUID> correctOptionIds,  // for choice — revealed post-submit
            List<String> acceptedAnswers  // for fill-blank — revealed post-submit
    ) {}
}
