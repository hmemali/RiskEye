package com.org.pfe.transaction_service.dto;

import com.org.pfe.transaction_service.entity.TransactionStatus;
import com.org.pfe.transaction_service.entity.TransactionType;
import com.org.pfe.transaction_service.entity.FraudReason;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private Long accountId;
    private TransactionType type;
    private BigDecimal amount;
    private TransactionStatus status;
    private String description;
    private LocalDateTime createdAt;
    private String transactionRef;
    private FraudReason riskReason;
    private int dailyTransactionCount;
    private int dailyLimit;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private String reviewerName;
}