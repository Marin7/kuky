package com.kuky.backend.placement.dto;

/** Content for the full-evaluation panel. No payment data is collected/shown in-app. */
public record FullEvaluationResponse(
        String writingPrompt,
        WritingSubmissionDto mySubmission, // null if not yet submitted
        WritingSectionDto writingSection
) {}
