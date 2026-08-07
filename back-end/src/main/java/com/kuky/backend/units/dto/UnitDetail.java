package com.kuky.backend.units.dto;

import com.kuky.backend.admin.dto.HomeworkAdminItem;
import com.kuky.backend.admin.dto.PresentationSummary;
import com.kuky.backend.admin.dto.StudentResponse;

import java.util.List;
import java.util.UUID;

public record UnitDetail(
        UUID id,
        String level,
        String subject,
        int position,
        List<PresentationSummary> presentations,
        List<HomeworkAdminItem> homeworks,
        List<StudentResponse> assignedStudents
) {}
