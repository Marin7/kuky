package com.kuky.backend.resources.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PurchaseRequest(
        @NotNull(message = "itemType es obligatorio.")
        @Pattern(regexp = "RESOURCE|BUNDLE", message = "itemType debe ser RESOURCE o BUNDLE.")
        String itemType,

        @NotBlank(message = "slug es obligatorio.")
        String slug
) {}
