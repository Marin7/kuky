package com.kuky.backend.learning.dto;

import java.util.UUID;

/** One activity of a unit PDF for the student (no score). {@code title} null → page fallback. */
public record ActivitySummary(
        UUID id,
        String title,
        int page,
        String format,
        String status
) {}
