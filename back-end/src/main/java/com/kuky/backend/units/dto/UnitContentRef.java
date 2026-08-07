package com.kuky.backend.units.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Lightweight type+id ref used in reorder requests. */
public record UnitContentRef(
        @NotBlank String type,
        @NotNull UUID id
) {}
