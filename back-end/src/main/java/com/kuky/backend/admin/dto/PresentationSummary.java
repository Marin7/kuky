package com.kuky.backend.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record PresentationSummary(
        UUID id,
        String title,
        boolean hasFile,
        String originalFileName,
        long sharedWith,
        Instant updatedAt
) {}
