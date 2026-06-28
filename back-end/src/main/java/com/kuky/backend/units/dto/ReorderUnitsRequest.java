package com.kuky.backend.units.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReorderUnitsRequest(
        @NotBlank String level,
        @NotNull List<UUID> orderedIds
) {}
