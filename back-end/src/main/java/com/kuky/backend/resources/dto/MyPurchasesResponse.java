package com.kuky.backend.resources.dto;

import java.util.List;

public record MyPurchasesResponse(
        String currency,
        List<PurchaseSummary> purchases
) {}
