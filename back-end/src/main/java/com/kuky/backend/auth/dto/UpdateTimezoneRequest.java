package com.kuky.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTimezoneRequest(
        @NotBlank(message = "La zona horaria es obligatoria.")
        String zone,
        boolean manual
) {}
