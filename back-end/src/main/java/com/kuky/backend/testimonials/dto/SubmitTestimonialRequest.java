package com.kuky.backend.testimonials.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitTestimonialRequest(
        @NotBlank(message = "El testimonio no puede estar vacío.")
        @Size(min = 10, max = 2000, message = "El testimonio debe tener al menos 10 caracteres.")
        String text
) {}
