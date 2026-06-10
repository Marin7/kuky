package com.kuky.backend.resources.dto;

import java.util.List;

public record CatalogResponse(
        String currency,
        List<ResourceCardDto> freeResources,
        List<ResourceCardDto> paidResources,
        List<BundleCardDto> bundles
) {}
