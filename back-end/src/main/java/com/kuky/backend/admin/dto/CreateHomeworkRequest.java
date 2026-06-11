package com.kuky.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateHomeworkRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String instructions,
        LocalDate dueOn,
        List<UUID> assigneeIds
) {}
