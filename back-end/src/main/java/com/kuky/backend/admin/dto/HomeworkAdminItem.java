package com.kuky.backend.admin.dto;

import java.util.List;
import java.util.UUID;

public record HomeworkAdminItem(
        UUID id,
        String title,
        String instructions,
        String homeworkType,
        String level,
        String format,                      // MANUAL | EXERCISE | MIXED (derived)
        String composition,                 // WRITE | ALL_MANUAL | ALL_AUTO | MIXED
        List<HomeworkQuestionDto> questions,
        String audioUrl,                    // listening homework external source (nullable)
        UUID audioFileId,                   // listening homework uploaded file (nullable)
        String audioFileName,               // original filename of the uploaded audio (nullable)
        String mediaSourceKind,             // AUDIO_URL | UPLOADED_FILE | VIDEO_PAGE | YOUTUBE | null
        List<String> labels,                // teacher-only organization labels (empty = unlabeled)
        List<AssigneeDto> assignees,
        boolean hasUnseenSubmissions
) {}
