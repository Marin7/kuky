package com.kuky.backend.resources.dto;

import java.time.Instant;
import java.util.List;

public record PurchaseSummary(
        String id,
        String itemType,
        String slug,
        String title,
        int amountCents,
        String receiptReference,
        Instant purchasedAt,
        List<String> grantedResourceSlugs
) {}
