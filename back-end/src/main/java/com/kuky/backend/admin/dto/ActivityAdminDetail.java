package com.kuky.backend.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ActivityAdminDetail(
        UUID id,
        String title,
        String format,
        String level,
        String homeworkType,
        UUID presentationId,
        String presentationTitle,
        int position,
        UUID triggerFileId,
        Integer triggerPage,
        String instructionsText,
        String youtubeUrl,
        UUID imageId,
        boolean hasInstructions,
        Instant createdAt,
        Instant updatedAt,
        List<HomeworkQuestionDto> questions,
        InstructionsMeta instructions
) {
    public record InstructionsMeta(
            UUID id,
            String originalName,
            String contentType,
            long byteSize
    ) {}
}
