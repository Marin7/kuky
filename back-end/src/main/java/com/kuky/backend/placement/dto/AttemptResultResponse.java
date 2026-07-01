package com.kuky.backend.placement.dto;

import java.util.List;

public record AttemptResultResponse(
        String status, // IN_PROGRESS | COMPLETED
        String overallCefr, // null until all sections are submitted
        List<SkillResultDto> skills
) {}
