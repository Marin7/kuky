package com.kuky.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SlideRequest(
        @NotBlank @Size(max = 200) String heading,
        @Size(max = 5000) String body,
        UUID imageId
) {}
