package com.org.pfe.account_service.controller;

import com.org.pfe.account_service.dto.AccountResponse;
import com.org.pfe.account_service.dto.CreateAccountRequest;
import com.org.pfe.account_service.dto.UpdateBalanceRequest;
import com.org.pfe.account_service.dto.UpdateStatusRequest;
import com.org.pfe.account_service.service.AccountService;
import com.org.pfe.account_service.security.AccountPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RestController
@RequestMapping({"/api/accounts", "/api/v1/accounts"})
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

        @Value("${account.service.api-key:}")
        private String accountServiceApiKey;

    /**
     * ADMIN :
     * Créer un compte bancaire.
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ) {

        AccountResponse response =
                accountService.createAccount(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * ADMIN :
     * Voir tous les comptes.
     */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {

        return ResponseEntity.ok(
                accountService.getAllAccounts()
        );
    }

    /**
     * Voir un compte.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(
                        @PathVariable Long id,
                        @RequestHeader(value = "X-Account-Service-Api-Key", required = false) String providedApiKey
    ) {
                AccountResponse account = accountService.getAccountById(id);
                if (isInternalAuthorized(providedApiKey)) return ResponseEntity.ok(account);
                AccountPrincipal principal = currentUser();
                if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                if (!isAdmin() && !principal.userId().equals(account.getOwnerId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
                return ResponseEntity.ok(account);
    }

    /**
     * CLIENT :
     * Voir les comptes appartenant à un utilisateur.
     */
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByOwner(
                        @PathVariable Long ownerId,
                        @RequestHeader(value = "X-Account-Service-Api-Key", required = false) String providedApiKey
    ) {
                if (isInternalAuthorized(providedApiKey)) {
                        return ResponseEntity.ok(accountService.getAccountsByOwner(ownerId));
                }
                AccountPrincipal principal = currentUser();
                if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                if (!isAdmin() && !principal.userId().equals(ownerId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
        return ResponseEntity.ok(
                accountService.getAccountsByOwner(ownerId)
        );
    }

        private boolean isInternalAuthorized(String providedApiKey) {
                return accountServiceApiKey != null && !accountServiceApiKey.isBlank()
                                && providedApiKey != null && MessageDigest.isEqual(
                                                accountServiceApiKey.getBytes(StandardCharsets.UTF_8),
                                                providedApiKey.getBytes(StandardCharsets.UTF_8));
        }

        private boolean isAdmin() {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                return authentication != null && authentication.getAuthorities().stream()
                                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        }

        private AccountPrincipal currentUser() {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
                        return null;
                }
                return principal;
        }

    /**
     * ADMIN :
     * Modifier le statut.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AccountResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {

        return ResponseEntity.ok(
                accountService.updateStatus(id, request)
        );
    }

    /**
     * ADMIN :
     * Modifier le solde.
     */
    @PatchMapping("/{id}/balance")
    public ResponseEntity<AccountResponse> updateBalance(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBalanceRequest request
    ) {

        return ResponseEntity.ok(
                accountService.updateBalance(id, request)
        );
    }

    /**
     * ADMIN :
     * Supprimer un compte.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable Long id
    ) {

        accountService.deleteAccount(id);

        return ResponseEntity.noContent().build();
    }
}