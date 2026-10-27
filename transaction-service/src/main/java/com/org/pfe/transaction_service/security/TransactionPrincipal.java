package com.org.pfe.transaction_service.security;

import java.security.Principal;

public record TransactionPrincipal(Long userId, String email, String role) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}