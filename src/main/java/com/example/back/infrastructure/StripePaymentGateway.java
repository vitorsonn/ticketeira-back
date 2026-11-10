package com.example.back.infrastructure;


import com.example.back.application.port.PaymentGateway;
import com.example.back.dto.PaymentInfo;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class StripePaymentGateway implements PaymentGateway {

    private final String secretKey;

    public StripePaymentGateway(
            @Value("${stripe.api.key}") String secretKey
    ) {
        this.secretKey = secretKey;
        Stripe.apiKey = secretKey;
    }

    @Override
    public String createPaymentIntent(BigDecimal price) {
        try {
            long amountInCents = price.multiply(new BigDecimal("100"))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("brl")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            return intent.getClientSecret();
        } catch (StripeException e) {
            throw new RuntimeException("Erro ao criar PaymentIntent no Stripe", e);
        }
    }

    @Override
    public PaymentInfo getPayment(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            return new PaymentInfo(
                    intent.getId(),
                    intent.getStatus(),
                    new BigDecimal(intent.getAmount()).divide(new BigDecimal(100))
            );
        } catch (StripeException e) {
            throw new RuntimeException("Erro ao buscar PaymentIntent no Stripe", e);
        }
    }

    @Override
    public BigDecimal getPaymentAmount(String paymentIntentId) {
        return getPayment(paymentIntentId).amount();
    }
}
