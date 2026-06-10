package com.kuky.backend.resources.dto;

public record AssetDto(
        String assetType,
        String label,
        String locator
) {}
