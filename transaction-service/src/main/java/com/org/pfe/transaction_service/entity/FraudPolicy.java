package com.org.pfe.transaction_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_policy")
@Getter
@Setter
@NoArgsConstructor
public class FraudPolicy {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private int maxDailyTransactions;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal amountThreshold;

    @Column(nullable = false, length = 64)
    private String timeZone;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}