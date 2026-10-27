package com.org.pfe.account_service.dto;

import com.org.pfe.account_service.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CreateAccountRequest {

    @NotNull(message = "Le type de compte est obligatoire")
    private AccountType accountType;

    @NotNull(message = "L'identifiant du propriétaire est obligatoire")
    private Long ownerId;

    @NotBlank(message = "Le nom du propriétaire est obligatoire")
    private String ownerFullName;

    @NotNull(message = "Le solde initial est obligatoire")
    @DecimalMin(value = "0.00", inclusive = true, message = "Le solde initial ne peut pas être négatif")
    private BigDecimal balance = BigDecimal.ZERO;

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(
            AccountType accountType,
            Long ownerId,
            String ownerFullName,
            BigDecimal balance
    ) {
        this.accountType = accountType;
        this.ownerId = ownerId;
        this.ownerFullName = ownerFullName;
        this.balance = balance;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}