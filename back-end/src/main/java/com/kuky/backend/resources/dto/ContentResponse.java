package com.kuky.backend.resources.dto;

import java.util.List;

public record ContentResponse(
        String slug,
        List<AssetDto> assets
) {}
