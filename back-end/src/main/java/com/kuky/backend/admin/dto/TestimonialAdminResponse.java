package com.kuky.backend.admin.dto;

import com.kuky.backend.testimonials.model.TestimonialStatus;

import java.time.Instant;
import java.util.UUID;

public record TestimonialAdminResponse(
        UUID id,
        String text,
        String studentName,
        TestimonialStatus status,
        int displayOrder,
        Instant submittedAt,
        Instant reviewedAt
) {}
