package com.kuky.backend.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SetAssigneesRequest(
        @NotNull List<UUID> assigneeIds
) {}
