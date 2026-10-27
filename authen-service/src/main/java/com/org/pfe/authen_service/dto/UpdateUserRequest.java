package com.org.pfe.authen_service.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String fullName;
    private String email;
    private String password; // optionnel — vide = ne pas changer
    private String role;
}