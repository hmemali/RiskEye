package com.org.pfe.transaction_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_notification_outbox", uniqueConstraints = {
        @UniqueConstraint(name = "uk_fraud_outbox_transaction", columnNames = "transaction_id")
})
@Getter
@Setter
@NoArgsConstructor
public class FraudNotificationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private Long transactionId;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 64)
    private String accountNumber;

    @Column(precision = 19, scale = 3, nullable = false)
    private BigDecimal amount;

    @Column(length = 254)
    private String recipientEmail;

    @Column(length = 160)
    private String recipientName;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, length = 48)
    private String riskReason;

    @Column(nullable = false)
    private int dailyTransactionCount;

    @Column(nullable = false)
    private int dailyLimit;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutboxDeliveryStatus deliveryStatus = OutboxDeliveryStatus.PENDING;

    @Column(nullable = false)
    private int attemptCount;

    @Column(length = 1000)
    private String lastError;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastAttemptAt;
}