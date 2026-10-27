package com.org.pfe.transaction_service.controller;

import com.org.pfe.transaction_service.dto.TransactionResponse;
import com.org.pfe.transaction_service.security.TransactionPrincipal;
import com.org.pfe.transaction_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/transactions")
@RequiredArgsConstructor
public class ClientTransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public org.springframework.data.domain.Page<TransactionResponse> getMine(
            @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int page,
            @RequestParam(defaultValue = "20") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int size,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type
    ) {
        TransactionPrincipal client = (TransactionPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return transactionService.getByOwner(client.userId(), accountId, status, type, page, size);
    }
}