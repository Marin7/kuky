package com.kuky.backend.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kuky.backend.admin.dto.SaveHomeworkFeedbackRequest;

import java.util.List;

public record QuizReviewRequest(
        List<SaveHomeworkFeedbackRequest.AnnotatedAnswerRequest> answers,
        String feedbackText,
        @JsonProperty("finalize") Boolean finalizeGrade
) {}
