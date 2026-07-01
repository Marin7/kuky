package com.kuky.backend.placement.dto;

import java.time.Instant;

/** Timing state of the Writing section, mirrors SectionDto for the auto-graded sections. */
public record WritingSectionDto(
        String status, // NOT_STARTED | IN_PROGRESS
        int timeLimitSeconds,
        Instant deadlineAt // null unless started
) {}
