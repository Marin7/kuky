package com.kuky.backend.admin.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HomeworkAdminItem(
        UUID id,
        String title,
        String instructions,
        LocalDate dueOn,
        List<AssigneeDto> assignees
) {}
