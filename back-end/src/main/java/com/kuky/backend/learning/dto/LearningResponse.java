package com.kuky.backend.learning.dto;

import java.util.List;

public record LearningResponse(
        List<PresentationBlockResponse> presentation,
        List<PastClassResponse> pastClasses,
        List<HomeworkItemResponse> homework
) {}
