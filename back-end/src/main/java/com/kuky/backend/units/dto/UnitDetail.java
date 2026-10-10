package com.kuky.backend.units.dto;

import com.kuky.backend.admin.dto.StudentResponse;

import java.util.List;
import java.util.UUID;

/** A unit as edited from its view: its PDF (null until uploaded), page activities, end quizzes, students. */
public record UnitDetail(
        UUID id,
        String level,
        String subject,
        int position,
        UnitPdfInfo pdf,
        List<UnitActivitySummary> activities,
        List<UnitQuizItem> quizzes,
        List<StudentResponse> assignedStudents
) {}
