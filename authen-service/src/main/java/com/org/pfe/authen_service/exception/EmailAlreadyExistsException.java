package com.org.pfe.authen_service.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Un compte existe deja avec l'email : " + email);
    }
}