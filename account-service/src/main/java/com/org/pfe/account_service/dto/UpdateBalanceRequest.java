package com.org.pfe.account_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class UpdateBalanceRequest {

    @NotNull(message = "Le solde est obligatoire")
    @DecimalMin(value = "0.00", inclusive = true, message = "Le solde ne peut pas être négatif")
    private BigDecimal balance;

    public UpdateBalanceRequest() {
    }

    public UpdateBalanceRequest(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
