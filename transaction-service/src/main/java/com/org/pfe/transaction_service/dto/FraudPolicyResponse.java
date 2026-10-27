package com.org.pfe.transaction_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FraudPolicyResponse(
        int maxDailyTransactions,
        BigDecimal amountThreshold,
        String timeZone,
        LocalDateTime updatedAt
) {
}