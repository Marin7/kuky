package com.kuky.backend.units.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUnitRequest(
        @NotNull String level,
        @NotBlank @Size(max = 200) String subject
) {}
