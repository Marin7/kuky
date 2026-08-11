package com.kuky.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kuky.backend.learning.model.FormattedTextSegment;

import java.util.List;
import java.util.UUID;

public record SaveHomeworkFeedbackRequest(
        String feedbackText,
        List<FormattedTextSegment> response,
        List<AnnotatedAnswerRequest> answers,
        /** WRITE only: 0–100 teacher percent (required on finalize). */
        Integer teacherScorePercent,
        /**
         * {@code true} = require all percents and set GRADED.
         * JSON field is {@code finalize} (Java name avoids clash with {@link Object#finalize()}).
         */
        @JsonProperty("finalize")
        Boolean finalizeGrade
) {
    public record AnnotatedAnswerRequest(
            UUID questionId,
            List<FormattedTextSegment> formatted,
            Integer teacherScorePercent
    ) {}
}
