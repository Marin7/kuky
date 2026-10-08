package com.kuky.backend.units.dto;

import com.kuky.backend.admin.dto.StudentResponse;

import java.util.List;
import java.util.UUID;

/** A unit as edited from its view: its PDF (null until uploaded), content order, page activities. */
public record UnitDetail(
        UUID id,
        String level,
        String subject,
        int position,
        UnitPdfInfo pdf,
        List<UnitContentItem> contents,
        List<UnitActivitySummary> activities,
        List<StudentResponse> assignedStudents
) {}
