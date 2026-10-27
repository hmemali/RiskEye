package com.org.pfe.notification_service.security;

import java.security.Principal;

public record NotificationPrincipal(Long userId, String email, String role) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}