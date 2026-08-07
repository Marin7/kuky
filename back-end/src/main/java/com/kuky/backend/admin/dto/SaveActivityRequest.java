package com.kuky.backend.admin.dto;

import java.util.List;
import java.util.UUID;

public record SaveActivityRequest(
        String title,
        UUID presentationId,
        String format,
        String level,
        String homeworkType,
        UUID triggerFileId,
        Integer triggerPage,
        String instructionsText,
        String youtubeUrl,
        UUID imageId,
        List<HomeworkQuestionDto> questions
) {}
