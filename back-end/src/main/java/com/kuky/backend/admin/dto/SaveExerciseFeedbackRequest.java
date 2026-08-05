package com.kuky.backend.admin.dto;

/** Plain-text teacher feedback for a graded exercise submission (empty clears). */
public record SaveExerciseFeedbackRequest(String feedback) {}
