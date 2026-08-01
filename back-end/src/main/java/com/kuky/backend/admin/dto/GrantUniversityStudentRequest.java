package com.kuky.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record GrantUniversityStudentRequest(@NotBlank String level) {}
