package com.kuky.backend.placement.dto;

import java.time.Instant;
import java.util.List;

/** Combined view for the teacher's student-detail page (FR-015): per-skill CEFR + Writing together. */
public record StudentEvaluationResponse(
        ResultDto result, // null if the student has not completed the auto-graded test
        List<WritingSubmissionDto> writing
) {
    public record ResultDto(String overallCefr, Instant completedAt, List<SkillResultDto> skills) {}
}
