package com.example.back.application.port;

import com.example.back.dto.PaymentInfo;

import java.math.BigDecimal;

public interface PaymentGateway {

    String createPaymentIntent(BigDecimal price);

    PaymentInfo getPayment(String paymentIntentId);

    BigDecimal getPaymentAmount(String paymentIntentId);
}
