package com.kuky.backend.learning.dto;

import java.util.UUID;

/** One activity of a unit PDF for the student (no score, no title of its own). */
public record ActivitySummary(
        UUID id,
        int page,
        String format,
        String status
) {}
