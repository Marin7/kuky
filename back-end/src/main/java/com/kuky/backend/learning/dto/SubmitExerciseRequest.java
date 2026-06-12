package com.kuky.backend.learning.dto;

import java.util.List;
import java.util.UUID;

/** A student's answers for a self-correcting exercise. */
public record SubmitExerciseRequest(
        List<AnswerDto> answers
) {
    public record AnswerDto(
            UUID questionId,
            List<UUID> selectedOptionIds, // choice questions; [] otherwise
            String answerText             // fill-blank; null otherwise
    ) {}
}
