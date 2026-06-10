package com.kuky.backend.learning.dto;

import jakarta.validation.constraints.Size;

public record SubmitHomeworkRequest(
        @Size(max = 2000, message = "La respuesta es demasiado larga (máximo 2000 caracteres).")
        String response
) {}
