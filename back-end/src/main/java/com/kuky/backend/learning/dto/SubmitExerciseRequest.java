package com.kuky.backend.learning.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.kuky.backend.learning.model.FormattedTextSegment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A student's answers for a self-correcting exercise. */
public record SubmitExerciseRequest(
        List<AnswerDto> answers,
        Instant contentRevisedAt
) {
    public SubmitExerciseRequest(List<AnswerDto> answers) {
        this(answers, null);
    }
    public record AnswerDto(
            UUID questionId,
            List<UUID> selectedOptionIds, // choice questions; [] otherwise
            JsonNode answerJson,          // structured kinds; null otherwise
            String text,                  // FREE_TEXT plain answer; null otherwise
            List<FormattedTextSegment> formatted // FREE_TEXT rich text (writing parity)
    ) {
        /** Back-compat for structured-only callers. */
        public AnswerDto(UUID questionId, List<UUID> selectedOptionIds, JsonNode answerJson) {
            this(questionId, selectedOptionIds, answerJson, null, null);
        }

        public AnswerDto(UUID questionId, List<UUID> selectedOptionIds, JsonNode answerJson, String text) {
            this(questionId, selectedOptionIds, answerJson, text, null);
        }
    }
}
