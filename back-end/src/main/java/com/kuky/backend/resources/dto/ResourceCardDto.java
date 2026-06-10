package com.kuky.backend.resources.dto;

public record ResourceCardDto(
        String slug,
        String title,
        String description,
        String level,
        String category,
        String pricing,
        Integer priceCents,
        boolean owned,
        boolean locked,
        String relatedResourceSlug
) {}
