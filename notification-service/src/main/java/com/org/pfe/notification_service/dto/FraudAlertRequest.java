package com.org.pfe.notification_service.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Data
public class FraudAlertRequest {
    @NotNull
    private Long ownerId;
    private String recipientEmail;
    private String recipientName;
    @NotNull
    private Long accountId;
    private String accountNumber;
    @NotNull
    private Long transactionId;
    @NotNull
    private BigDecimal amount;
    private String riskReason;
    @PositiveOrZero
    private int dailyTransactionCount;
    @PositiveOrZero
    private int dailyLimit;
    private java.time.LocalDateTime occurredAt;
    private String message;
}