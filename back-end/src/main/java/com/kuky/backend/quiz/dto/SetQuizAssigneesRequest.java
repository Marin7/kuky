package com.kuky.backend.quiz.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SetQuizAssigneesRequest(@NotNull List<UUID> studentIds) {}
