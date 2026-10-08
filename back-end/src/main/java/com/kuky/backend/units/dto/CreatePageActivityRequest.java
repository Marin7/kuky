package com.kuky.backend.units.dto;

import com.kuky.backend.admin.dto.HomeworkQuestionDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreatePageActivityRequest(
        @NotNull @Min(1) Integer page,
        String title,
        @NotNull List<HomeworkQuestionDto> questions
) {}
