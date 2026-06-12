package com.kuky.backend.admin.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HomeworkAdminItem(
        UUID id,
        String title,
        String instructions,
        LocalDate dueOn,
        String homeworkType,
        String level,
        String format,                      // MANUAL | EXERCISE
        List<HomeworkQuestionDto> questions, // empty for MANUAL; with answer key for EXERCISE
        List<AssigneeDto> assignees
) {}
