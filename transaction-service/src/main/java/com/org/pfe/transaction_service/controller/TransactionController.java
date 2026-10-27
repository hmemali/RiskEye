package com.org.pfe.transaction_service.controller;

import com.org.pfe.transaction_service.dto.CreateTransactionRequest;
import com.org.pfe.transaction_service.dto.TransactionResponse;
import com.org.pfe.transaction_service.service.TransactionService;
import com.org.pfe.transaction_service.security.TransactionPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/transactions", "/api/v1/transactions"})
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody CreateTransactionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                transactionService.createTransaction(request, currentUser().userId(), idempotencyKey));
    }

    @GetMapping("/me")
    public ResponseEntity<List<TransactionResponse>> getMine() {
        return ResponseEntity.ok(transactionService.getByOwner(currentUser().userId()));
    }

    @GetMapping("/suspicious")
    public ResponseEntity<List<TransactionResponse>> getSuspicious() {
        return ResponseEntity.ok(transactionService.getSuspicious());
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAll() {
        return ResponseEntity.ok(transactionService.getAll());
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionResponse>> getByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.getByAccount(accountId));
    }

    private TransactionPrincipal currentUser() {
        return (TransactionPrincipal) org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
    }
}