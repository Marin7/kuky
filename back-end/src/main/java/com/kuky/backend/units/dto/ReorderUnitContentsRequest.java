package com.kuky.backend.units.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReorderUnitContentsRequest(
        @NotNull @Valid List<UnitContentRef> items
) {}
