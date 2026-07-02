package com.kuky.backend.admin.dto;

import java.util.UUID;

public record UnitProgressDto(
        UUID unitId,
        String subject,
        String level,
        int totalHomeworks,
        int completedHomeworks,
        boolean complete
) {}
