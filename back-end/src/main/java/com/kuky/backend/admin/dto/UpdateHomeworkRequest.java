package com.kuky.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record UpdateHomeworkRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String instructions,
        LocalDate dueOn,
        String homeworkType,
        String level,
        String format,                       // MANUAL | EXERCISE (null ⇒ MANUAL)
        List<HomeworkQuestionDto> questions  // required non-empty when format == EXERCISE
) {}
