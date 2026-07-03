package com.kuky.backend.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttachCompanionStudentRequest(@NotNull UUID studentId) {}
