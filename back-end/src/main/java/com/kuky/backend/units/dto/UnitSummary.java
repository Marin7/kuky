package com.kuky.backend.units.dto;

import java.util.List;
import java.util.UUID;

public record UnitSummary(
        UUID id,
        String level,
        String subject,
        int position,
        int presentationCount,
        int homeworkCount,
        List<String> assignedStudentIds
) {}
