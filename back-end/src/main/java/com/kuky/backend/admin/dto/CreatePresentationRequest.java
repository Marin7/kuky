package com.kuky.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePresentationRequest(
        @NotBlank @Size(max = 200) String title,
        String level
) {}
