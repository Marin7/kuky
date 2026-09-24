package com.kuky.backend.preferences.dto;

import jakarta.validation.constraints.NotNull;

/** Body of {@code PUT /api/v1/me/email-preferences/{type}}. */
public record UpdateEmailPreferenceRequest(
        @NotNull Boolean enabled
) {}
