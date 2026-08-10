package com.kuky.backend.admin.dto;

import java.util.UUID;

import com.kuky.backend.learning.model.FormattedTextSegment;

import java.util.List;

public record SaveHomeworkFeedbackRequest(
        String feedbackText,
        List<FormattedTextSegment> response,
        List<AnnotatedAnswerRequest> answers
) {
    public record AnnotatedAnswerRequest(UUID questionId, List<FormattedTextSegment> formatted) {}
}
