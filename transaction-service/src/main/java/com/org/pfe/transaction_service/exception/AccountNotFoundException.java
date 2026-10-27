package com.org.pfe.transaction_service.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long accountId) {
        super("Le compte avec l'id " + accountId + " n'existe pas");
    }
}