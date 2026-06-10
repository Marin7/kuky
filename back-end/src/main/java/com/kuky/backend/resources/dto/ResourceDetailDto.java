package com.kuky.backend.resources.dto;

public record ResourceDetailDto(
        String slug,
        String title,
        String description,
        String level,
        String category,
        String pricing,
        Integer priceCents,
        String currency,
        String previewText,
        boolean owned,
        boolean locked,
        String relatedResourceSlug
) {}
