package com.kuky.backend.resources.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class PlaceholderPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(PlaceholderPaymentProvider.class);

    @Override
    public PaymentResult authorize(long amountCents, String currency, String reference) {
        String synthetic = "placeholder-" + UUID.randomUUID();
        log.warn("PlaceholderPaymentProvider — no real payment taken for reference '{}' ({} {} cents). Granting access immediately.",
                reference, amountCents, currency);
        return new PaymentResult(true, synthetic);
    }
}
