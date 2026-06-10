package com.kuky.backend.resources.payment;

public interface PaymentProvider {

    record PaymentResult(boolean approved, String providerReference) {}

    PaymentResult authorize(long amountCents, String currency, String reference);
}
