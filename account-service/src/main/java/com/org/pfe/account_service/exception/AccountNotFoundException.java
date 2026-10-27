package com.org.pfe.account_service.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long id) {
        super("Compte bancaire introuvable avec l'id : " + id);
    }
}