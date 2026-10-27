package com.org.pfe.transaction_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FraudDecisionRequest(
        @NotNull Decision decision,
        @Size(max = 500) String note
) {
    public enum Decision {
        APPROVED,
        REJECTED
    }
}