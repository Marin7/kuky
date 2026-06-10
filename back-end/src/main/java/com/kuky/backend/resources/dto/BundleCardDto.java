package com.kuky.backend.resources.dto;

import java.util.List;

public record BundleCardDto(
        String slug,
        String title,
        String description,
        int priceCents,
        List<String> resourceSlugs,
        boolean owned
) {}
