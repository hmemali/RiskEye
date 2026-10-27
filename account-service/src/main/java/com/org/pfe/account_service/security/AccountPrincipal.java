package com.org.pfe.account_service.security;

import java.security.Principal;

public record AccountPrincipal(Long userId, String email, String role) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}