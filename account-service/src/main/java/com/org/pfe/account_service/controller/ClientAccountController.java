package com.org.pfe.account_service.controller;

import com.org.pfe.account_service.dto.AccountResponse;
import com.org.pfe.account_service.security.AccountPrincipal;
import com.org.pfe.account_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/accounts")
@RequiredArgsConstructor
public class ClientAccountController {

    private final AccountService accountService;

    @GetMapping
    public List<AccountResponse> getMine() {
        AccountPrincipal client = (AccountPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return accountService.getAccountsByOwner(client.userId());
    }
}