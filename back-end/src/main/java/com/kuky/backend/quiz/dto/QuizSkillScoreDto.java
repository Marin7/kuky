package com.kuky.backend.quiz.dto;

public record QuizSkillScoreDto(
        String skill,
        Integer scorePercent,
        Integer fullyCorrectCount,
        Integer questionUnitCount,
        boolean awaitingTeacher
) {}
