package com.kuky.backend.learning.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PastClassResponse(
        UUID id,
        String title,
        LocalDate heldOn,
        String teacherNote
) {}
