package com.kuky.backend.quiz.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.kuky.backend.admin.dto.HomeworkQuestionDto;

import java.util.List;
import java.util.UUID;

/** Teacher-facing quiz question — homework shape plus skill and per-question listening media. */
public record QuizQuestionDto(
        UUID id,
        String skill,
        String kind,
        String prompt,
        List<HomeworkQuestionDto.OptionDto> options,
        JsonNode structure,
        String mediaSourceKind,
        String audioUrl,
        UUID audioFileId
) {}
