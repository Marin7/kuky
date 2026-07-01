package com.kuky.backend.placement.dto;

import java.util.List;
import java.util.UUID;

public record SubmitSectionRequest(List<AnswerDto> answers) {
    public record AnswerDto(
            UUID questionId,
            List<UUID> selectedOptionIds, // choice questions; [] otherwise
            String answerText             // fill-blank; null otherwise
    ) {}
}
