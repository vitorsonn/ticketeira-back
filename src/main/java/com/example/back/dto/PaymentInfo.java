package com.example.back.dto;

import java.math.BigDecimal;

public record PaymentInfo(String id,
                          String status,
                          BigDecimal amount) {
}
