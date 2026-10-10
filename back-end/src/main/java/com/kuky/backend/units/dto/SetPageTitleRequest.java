package com.kuky.backend.units.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetPageTitleRequest(
        @NotBlank @Size(max = 200) String title
) {}
