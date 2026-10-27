package com.org.pfe.notification_service.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FraudAlertEvent(
        Long accountId,
        String accountNumber,
        Long transactionId,
        BigDecimal amount,
        String message,
        Instant receivedAt
) {
}