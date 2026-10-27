package com.org.pfe.authen_service.controller;

import com.org.pfe.authen_service.dto.UserResponse;
import com.org.pfe.authen_service.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserAdminService userService;

    @Value("${security.internal.api-key:}")
    private String internalApiKey;

    @GetMapping("/{id}/contact")
    public ResponseEntity<UserResponse> getContact(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String providedApiKey
    ) {
        if (internalApiKey.isBlank() || providedApiKey == null || !MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8),
                providedApiKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(userService.getUserById(id));
    }
}