package com.org.pfe.notification_service.entity;

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
import java.time.Instant;

@Entity
@Table(name = "client_notifications", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_transaction", columnNames = "transaction_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(name = "recipient_email", length = 254)
    private String recipientEmail;

    @Column(name = "recipient_name", length = 160)
    private String recipientName;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private Long transactionId;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal amount;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "risk_reason", length = 48)
    private String riskReason;

    @Column(name = "daily_transaction_count")
    private int dailyTransactionCount;

    @Column(name = "daily_limit")
    private int dailyLimit;

    @Column(name = "transaction_occurred_at")
    private Instant transactionOccurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_status", nullable = false, length = 16)
    private EmailStatus emailStatus = EmailStatus.PENDING;

    @Column(name = "email_attempts", nullable = false)
    private int emailAttempts;

    @Column(name = "last_email_attempt_at")
    private Instant lastEmailAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "read_at")
    private Instant readAt;
}