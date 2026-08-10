package com.kuky.backend.learning.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

/** A student's answers for a self-correcting exercise. */
public record SubmitExerciseRequest(
        List<AnswerDto> answers
) {
    public record AnswerDto(
            UUID questionId,
            List<UUID> selectedOptionIds, // choice questions; [] otherwise
            JsonNode answerJson,          // structured kinds; null otherwise
            String text                   // FREE_TEXT plain answer; null otherwise
    ) {
        /** Back-compat for structured-only callers. */
        public AnswerDto(UUID questionId, List<UUID> selectedOptionIds, JsonNode answerJson) {
            this(questionId, selectedOptionIds, answerJson, null);
        }
    }
}
