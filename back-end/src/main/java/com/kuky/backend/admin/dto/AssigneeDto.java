package com.kuky.backend.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AssigneeDto(
        UUID userId,
        String email,
        String status,
        String responseText,
        Instant submittedAt
) {}
