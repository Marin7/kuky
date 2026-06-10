package com.kuky.backend.resources.dto;

import java.time.Instant;
import java.util.List;

public record PurchaseResponse(
        String id,
        String itemType,
        String slug,
        String title,
        int amountCents,
        String currency,
        String receiptReference,
        String paymentProvider,
        List<String> grantedResourceSlugs,
        Instant purchasedAt
) {}
