package com.org.pfe.transaction_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_transaction_owner_idempotency", columnNames = {"owner_id", "idempotency_key"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "idempotency_key", length = 80)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(length = 48)
    private FraudReason riskReason;

    @Column(nullable = false)
    @Builder.Default
    private int dailyTransactionCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int dailyLimit = 0;

    private Long reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(length = 500)
    private String reviewNote;

    @Column(length = 254)
    private String reviewerName;

    private String description;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}