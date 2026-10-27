package com.org.pfe.account_service.dto;

import com.org.pfe.account_service.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateStatusRequest {

    @NotNull(message = "Le statut est obligatoire")
    private AccountStatus status;

    public UpdateStatusRequest() {
    }

    public UpdateStatusRequest(AccountStatus status) {
        this.status = status;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}