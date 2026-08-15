package com.kuky.backend.quiz.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.kuky.backend.learning.dto.ExerciseQuestionDto;
import com.kuky.backend.learning.dto.ExerciseResultResponse;
import com.kuky.backend.learning.model.FormattedTextSegment;

import java.util.List;
import java.util.UUID;

public record QuizTakeResponse(
        UUID id,
        String title,
        String description,
        String status,
        List<QuizStudentQuestionDto> questions,
        Integer scorePercent,
        Integer fullyCorrectCount,
        Integer questionUnitCount,
        List<QuizSkillScoreDto> skills,
        List<QuizQuestionResultDto> results,
        String feedback
) {
    public record QuizStudentQuestionDto(
            UUID id,
            String skill,
            String kind,
            String prompt,
            List<ExerciseQuestionDto.StudentOptionDto> options,
            JsonNode structure,
            String mediaSourceKind,
            String audioUrl,
            UUID audioFileId
    ) {}

    public record QuizQuestionResultDto(
            UUID questionId,
            String skill,
            double score,
            boolean correct,
            List<UUID> correctOptionIds,
            List<String> acceptedAnswers,
            List<ExerciseResultResponse.UnitResultDto> unitResults,
            List<UUID> selectedOptionIds,
            String answerText,
            Integer teacherPercent,
            List<FormattedTextSegment> formatted
    ) {}
}
