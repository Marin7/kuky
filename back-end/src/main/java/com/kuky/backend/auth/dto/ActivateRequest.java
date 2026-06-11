package com.kuky.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ActivateRequest(@NotBlank String token) {}
