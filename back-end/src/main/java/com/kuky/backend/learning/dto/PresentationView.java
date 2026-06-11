package com.kuky.backend.learning.dto;

import java.util.List;
import java.util.UUID;

/** A shared presentation as a student sees it (no sharing/admin metadata). */
public record PresentationView(
        UUID id,
        String title,
        List<SlideView> slides
) {
    public record SlideView(String heading, String body, UUID imageId, int sortOrder) {}
}
