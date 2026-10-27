package com.org.pfe.account_service.service;

import com.org.pfe.account_service.dto.AccountResponse;
import com.org.pfe.account_service.dto.CreateAccountRequest;
import com.org.pfe.account_service.dto.UpdateBalanceRequest;
import com.org.pfe.account_service.dto.UpdateStatusRequest;
import com.org.pfe.account_service.entity.Account;
import com.org.pfe.account_service.exception.AccountNotFoundException;
import com.org.pfe.account_service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    /**
     * Créer un nouveau compte bancaire.
     */
    public AccountResponse createAccount(CreateAccountRequest request) {

        String accountNumber = generateAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .accountType(request.getAccountType())
                .ownerId(request.getOwnerId())
                .ownerFullName(request.getOwnerFullName())
                .balance(request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO)
                .build();

        Account savedAccount = accountRepository.save(account);

        return toResponse(savedAccount);
    }

    /**
     * Récupérer tous les comptes.
     */
    public List<AccountResponse> getAllAccounts() {

        return accountRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer un compte par son ID.
     */
    public AccountResponse getAccountById(Long id) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        return toResponse(account);
    }

    /**
     * Récupérer les comptes d'un client.
     */
    public List<AccountResponse> getAccountsByOwner(Long ownerId) {

        return accountRepository.findByOwnerId(ownerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Modifier le statut d'un compte.
     */
    public AccountResponse updateStatus(
            Long id,
            UpdateStatusRequest request
    ) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        account.setStatus(request.getStatus());

        Account updatedAccount = accountRepository.save(account);

        return toResponse(updatedAccount);
    }

    /**
     * Modifier le solde d'un compte.
     */
    public AccountResponse updateBalance(Long id, UpdateBalanceRequest request) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        if (request.getBalance() == null || request.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le solde ne peut pas être négatif");
        }

        account.setBalance(request.getBalance());

        Account updatedAccount = accountRepository.save(account);

        return toResponse(updatedAccount);
    }

    /**
     * Supprimer un compte.
     */
    public void deleteAccount(Long id) {

        if (!accountRepository.existsById(id)) {
            throw new AccountNotFoundException(id);
        }

        accountRepository.deleteById(id);
    }

    /**
     * Génération automatique du numéro de compte.
     */
    private String generateAccountNumber() {

        String accountNumber;

        do {
            accountNumber =
                    "RE-" +
                            UUID.randomUUID()
                                    .toString()
                                    .substring(0, 8)
                                    .toUpperCase();

        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }

    /**
     * Conversion Entity -> DTO.
     */
    private AccountResponse toResponse(Account account) {

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getStatus(),
                account.getOwnerId(),
                account.getOwnerFullName(),
                account.getCreatedAt()
        );
    }
}