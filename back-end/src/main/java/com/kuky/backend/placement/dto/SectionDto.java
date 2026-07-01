package com.kuky.backend.placement.dto;

import java.time.Instant;
import java.util.List;

public record SectionDto(
        String skill,
        int timeLimitSeconds,
        String status, // NOT_STARTED | IN_PROGRESS | SUBMITTED
        Instant deadlineAt, // null unless started
        List<StudentQuestionDto> questions
) {}
