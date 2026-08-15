package com.kuky.backend.quiz.dto;

import com.kuky.backend.learning.dto.SubmitExerciseRequest;

import java.util.List;

public record SubmitQuizRequest(List<SubmitExerciseRequest.AnswerDto> answers) {}
