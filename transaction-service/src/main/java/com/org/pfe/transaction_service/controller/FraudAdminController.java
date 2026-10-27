package com.org.pfe.transaction_service.controller;

import com.org.pfe.transaction_service.dto.FraudDecisionRequest;
import com.org.pfe.transaction_service.dto.FraudPolicyRequest;
import com.org.pfe.transaction_service.dto.FraudPolicyResponse;
import com.org.pfe.transaction_service.dto.TransactionResponse;
import com.org.pfe.transaction_service.security.TransactionPrincipal;
import com.org.pfe.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/admin", "/api/v1/admin"})
@RequiredArgsConstructor
public class FraudAdminController {

    private final TransactionService transactionService;

    @GetMapping("/fraud-alerts")
    public List<TransactionResponse> getFraudAlerts(
            @RequestParam(defaultValue = "OPEN") String status
    ) {
        return transactionService.getFraudAlerts(status);
    }

    @GetMapping("/fraud-alerts/{id}")
    public TransactionResponse getFraudAlert(@PathVariable Long id) {
        return transactionService.getFraudAlert(id);
    }

    @PostMapping("/fraud-alerts/{id}/decision")
    public TransactionResponse decide(
            @PathVariable Long id,
            @Valid @RequestBody FraudDecisionRequest request
    ) {
        TransactionPrincipal admin = (TransactionPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return transactionService.decideFraudAlert(id, request, admin);
    }

    @GetMapping("/fraud-policy")
    public FraudPolicyResponse getFraudPolicy() {
        return transactionService.getFraudPolicy();
    }

    @PutMapping("/fraud-policy")
    public FraudPolicyResponse updateFraudPolicy(@Valid @RequestBody FraudPolicyRequest request) {
        return transactionService.updateFraudPolicy(request);
    }
}