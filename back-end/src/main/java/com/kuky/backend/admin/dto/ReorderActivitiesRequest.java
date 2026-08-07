package com.kuky.backend.admin.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReorderActivitiesRequest(
        @NotEmpty List<UUID> activityIds
) {}
