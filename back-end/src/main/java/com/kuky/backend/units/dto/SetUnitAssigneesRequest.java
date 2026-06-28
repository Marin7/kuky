package com.kuky.backend.units.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SetUnitAssigneesRequest(
        @NotNull List<UUID> studentIds
) {}
