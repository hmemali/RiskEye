package com.org.pfe.notification_service.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        Long recipientUserId,
        Long accountId,
        String accountNumber,
        Long transactionId,
        BigDecimal amount,
        String riskReason,
        int dailyTransactionCount,
        int dailyLimit,
        String emailStatus,
        int emailAttempts,
        boolean read,
        Instant createdAt
) {
}