package com.kuky.backend.resources.dto;

import java.time.Instant;
import java.util.List;

public record ReceiptResponse(
        String receiptReference,
        Instant purchasedAt,
        String buyerEmail,
        String itemType,
        String itemTitle,
        List<ReceiptLineItem> lineItems,
        int amountCents,
        String currency,
        String seller
) {}
