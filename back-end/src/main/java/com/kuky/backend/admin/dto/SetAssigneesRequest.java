package com.kuky.backend.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SetAssigneesRequest(
        @NotNull List<UUID> assigneeIds,
        LocalDate dueOn,
        List<DueOn> dueOns
) {
    /** Per-student due date when assigning; {@code dueOn} null means no deadline. */
    public record DueOn(UUID userId, LocalDate dueOn) {}
}
