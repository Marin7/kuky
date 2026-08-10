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
        String format,                      // MANUAL | EXERCISE | MIXED (derived)
        String composition,                 // WRITE | ALL_MANUAL | ALL_AUTO | MIXED
        List<HomeworkQuestionDto> questions,
        String audioUrl,                    // listening homework external source (nullable)
        UUID audioFileId,                   // listening homework uploaded file (nullable)
        String audioFileName,               // original filename of the uploaded audio (nullable)
        List<AssigneeDto> assignees
) {}
