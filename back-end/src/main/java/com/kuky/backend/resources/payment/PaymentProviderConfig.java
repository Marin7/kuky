package com.kuky.backend.resources.payment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentProviderConfig {

    @Bean
    public PaymentProvider paymentProvider() {
        // Runtime selection: placeholder now, real processor (e.g. Stripe) later.
        // Deliberately NOT using @ConditionalOnProperty — Spring Boot 4 treats an
        // empty-string property as "present", which causes both beans to register.
        return new PlaceholderPaymentProvider();
    }
}
