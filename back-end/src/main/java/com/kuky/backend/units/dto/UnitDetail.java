package com.kuky.backend.units.dto;

import com.kuky.backend.admin.dto.StudentResponse;

import java.util.List;
import java.util.UUID;

public record UnitDetail(
        UUID id,
        String level,
        String subject,
        int position,
        List<UnitContentItem> contents,
        List<StudentResponse> assignedStudents
) {}
