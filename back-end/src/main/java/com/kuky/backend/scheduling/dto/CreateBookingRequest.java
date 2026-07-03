package com.kuky.backend.scheduling.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateBookingRequest(
        @NotNull(message = "El inicio de la clase es obligatorio.")
        Instant slotStart,
        @NotNull(message = "La duración de la clase es obligatoria.")
        Integer durationMinutes
) {}
