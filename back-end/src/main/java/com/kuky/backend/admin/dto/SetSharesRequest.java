package com.kuky.backend.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SetSharesRequest(
        @NotNull List<UUID> studentIds
) {}
