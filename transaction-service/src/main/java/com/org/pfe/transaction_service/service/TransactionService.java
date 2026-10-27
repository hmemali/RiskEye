package com.org.pfe.transaction_service.service;

import com.org.pfe.transaction_service.client.AccountClient;
import com.org.pfe.transaction_service.client.AccountOwnerResponse;
import com.org.pfe.transaction_service.client.NotificationClient;
import com.org.pfe.transaction_service.client.UserClient;
import com.org.pfe.transaction_service.client.UserContactResponse;
import com.org.pfe.transaction_service.dto.CreateTransactionRequest;
import com.org.pfe.transaction_service.dto.FraudDecisionRequest;
import com.org.pfe.transaction_service.dto.FraudPolicyRequest;
import com.org.pfe.transaction_service.dto.FraudPolicyResponse;
import com.org.pfe.transaction_service.dto.TransactionResponse;
import com.org.pfe.transaction_service.entity.FraudPolicy;
import com.org.pfe.transaction_service.entity.FraudNotificationOutbox;
import com.org.pfe.transaction_service.entity.OutboxDeliveryStatus;
import com.org.pfe.transaction_service.entity.Transaction;
import com.org.pfe.transaction_service.entity.FraudReason;
import com.org.pfe.transaction_service.entity.TransactionStatus;
import com.org.pfe.transaction_service.exception.AccountNotFoundException;
import com.org.pfe.transaction_service.repository.FraudPolicyRepository;
import com.org.pfe.transaction_service.repository.FraudNotificationOutboxRepository;
import com.org.pfe.transaction_service.repository.TransactionRepository;
import com.org.pfe.transaction_service.security.TransactionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.DateTimeException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final FraudPolicyRepository fraudPolicyRepository;
    private final FraudNotificationOutboxRepository fraudNotificationOutboxRepository;
    private final AccountClient accountClient;
    private final NotificationClient notificationClient;
    private final UserClient userClient;

    @Value("${fraud.max-daily-transactions}")
    private int maxDailyTransactions;

    @Value("${fraud.amount-threshold}")
    private BigDecimal amountThreshold;

    @Value("${fraud.time-zone:Africa/Tunis}")
    private String fraudTimeZone;

    @Value("${fraud.notification.max-attempts:10}")
    private int maxNotificationAttempts;

    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        return createTransaction(request, null, null);
    }

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request, Long ownerId) {
        return createTransaction(request, ownerId, null);
    }

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request, Long ownerId, String idempotencyKey) {
        AccountOwnerResponse account = accountClient.getAccount(request.getAccountId());
        if (account == null) {
            throw new AccountNotFoundException(request.getAccountId());
        }
        if (ownerId != null && !ownerId.equals(account.ownerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce compte ne vous appartient pas.");
        }
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (idempotencyKey.length() > 80) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clé d'idempotence dépasse 80 caractères.");
            }
            Transaction existing = transactionRepository.findByOwnerIdAndIdempotencyKey(account.ownerId(), idempotencyKey)
                    .orElse(null);
                if (existing != null) {
                boolean sameRequest = existing.getAccountId().equals(request.getAccountId())
                    && existing.getType() == request.getType()
                    && existing.getAmount().compareTo(request.getAmount()) == 0
                    && Objects.equals(existing.getDescription(), request.getDescription());
                if (!sameRequest) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cette clé d'idempotence est déjà associée à une autre transaction.");
                }
                return toResponse(existing);
                }
        }

        FraudPolicy policy = currentFraudPolicy();
        int dailyLimit = policy.getMaxDailyTransactions();
        BigDecimal activeAmountThreshold = policy.getAmountThreshold();

        LocalDate today = LocalDate.now(ZoneId.of(policy.getTimeZone()));
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();
        Set<Long> ownerAccountIds = accountClient.getAccountsByOwner(account.ownerId()).stream()
            .map(AccountOwnerResponse::id)
            .filter(id -> id != null)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        ownerAccountIds.add(request.getAccountId());
        long todayCount = transactionRepository.countTodayTransactions(
            List.copyOf(ownerAccountIds), startOfDay, startOfNextDay);

        boolean tooManyToday = todayCount >= dailyLimit;
        boolean amountTooHigh = request.getAmount().compareTo(activeAmountThreshold) > 0;
        boolean isSuspect = tooManyToday || amountTooHigh;
        int transactionNumberToday = Math.toIntExact(todayCount + 1);
        FraudReason riskReason = tooManyToday && amountTooHigh
            ? FraudReason.MULTIPLE_RISK_SIGNALS
            : tooManyToday ? FraudReason.DAILY_TRANSACTION_LIMIT_EXCEEDED
            : amountTooHigh ? FraudReason.HIGH_VALUE_AMOUNT : null;

        Transaction transaction = Transaction.builder()
                .accountId(request.getAccountId())
            .ownerId(account.ownerId())
            .idempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey)
                .type(request.getType())
                .amount(request.getAmount())
                .description(request.getDescription())
                .status(isSuspect ? TransactionStatus.SUSPECT : TransactionStatus.NORMAL)
                .riskReason(riskReason)
                .dailyTransactionCount(transactionNumberToday)
                .dailyLimit(dailyLimit)
                .createdAt(LocalDateTime.now(ZoneId.of(policy.getTimeZone())))
                .build();

        Transaction saved = transactionRepository.save(transaction);

        if (isSuspect) {
            UserContactResponse owner = userClient.getUser(account.ownerId());
            String message = tooManyToday
                    ? "Vous avez effectué " + transactionNumberToday + " transactions aujourd'hui (limite : "
                        + dailyLimit + "). La transaction de " + saved.getAmount()
                        + " DT a été signalée comme suspecte."
                    : "Une transaction de " + saved.getAmount() + " DT dépasse le seuil de montant autorisé et a été signalée comme suspecte.";
                FraudNotificationOutbox outbox = new FraudNotificationOutbox();
                outbox.setTransactionId(saved.getId());
                outbox.setOwnerId(account.ownerId());
                outbox.setAccountId(saved.getAccountId());
                outbox.setAccountNumber(account.accountNumber());
                outbox.setAmount(saved.getAmount());
                outbox.setRecipientEmail(owner == null ? null : owner.email());
                outbox.setRecipientName(account.ownerFullName());
                outbox.setMessage(message);
                outbox.setRiskReason(riskReason.name());
                outbox.setDailyTransactionCount(transactionNumberToday);
                outbox.setDailyLimit(dailyLimit);
                outbox.setOccurredAt(saved.getCreatedAt());
                fraudNotificationOutboxRepository.save(outbox);
        }

        return toResponse(saved);
    }

    public List<TransactionResponse> getByAccount(Long accountId) {
        return transactionRepository.findByAccountId(accountId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

        public List<TransactionResponse> getByOwner(Long ownerId) {
        List<Long> accountIds = accountClient.getAccountsByOwner(ownerId).stream()
            .map(AccountOwnerResponse::id)
            .filter(id -> id != null)
            .toList();
        if (accountIds.isEmpty()) return List.of();
        return transactionRepository.findByAccountIdInOrderByCreatedAtDesc(accountIds).stream()
            .map(this::toResponse)
            .toList();
        }

    public List<TransactionResponse> getAll() {
        return transactionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Page<TransactionResponse> getByOwner(
            Long ownerId,
            Long accountId,
            String status,
            String type,
            int page,
            int size
    ) {
        List<Long> accountIds = accountClient.getAccountsByOwner(ownerId).stream()
                .map(AccountOwnerResponse::id)
                .filter(id -> id != null)
                .toList();
        if (accountId != null) {
            if (!accountIds.contains(accountId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce compte ne vous appartient pas.");
            }
            accountIds = List.of(accountId);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (accountIds.isEmpty()) return Page.empty(pageable);
        TransactionStatus requestedStatus = parseStatus(status);
        com.org.pfe.transaction_service.entity.TransactionType requestedType = parseType(type);
        Page<Transaction> transactions = transactionRepository.findClientTransactions(
                accountIds, requestedStatus, requestedType, pageable);
        return transactions.map(this::toResponse);
    }

    private TransactionStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return null;
        try {
            return TransactionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut de transaction invalide.");
        }
    }

    private com.org.pfe.transaction_service.entity.TransactionType parseType(String type) {
        if (type == null || type.isBlank() || "ALL".equalsIgnoreCase(type)) return null;
        try {
            return com.org.pfe.transaction_service.entity.TransactionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de transaction invalide.");
        }
    }

    public List<TransactionResponse> getSuspicious() {
        return transactionRepository.findByStatusOrderByCreatedAtDesc(TransactionStatus.SUSPECT).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getFraudAlerts(String status) {
        List<Transaction> alerts;
        if (status == null || status.isBlank() || "OPEN".equalsIgnoreCase(status)) {
            alerts = transactionRepository.findByStatusOrderByCreatedAtDesc(TransactionStatus.SUSPECT);
        } else if ("ALL".equalsIgnoreCase(status)) {
            alerts = transactionRepository.findAll().stream()
                    .filter(transaction -> transaction.getRiskReason() != null)
                    .sorted((first, second) -> second.getCreatedAt().compareTo(first.getCreatedAt()))
                    .toList();
        } else {
            TransactionStatus requestedStatus;
            try {
                requestedStatus = TransactionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut d'alerte invalide.");
            }
            alerts = transactionRepository.findByStatusOrderByCreatedAtDesc(requestedStatus).stream()
                    .filter(transaction -> transaction.getRiskReason() != null)
                    .toList();
        }
        return alerts.stream().map(this::toResponse).toList();
    }

    public TransactionResponse getFraudAlert(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .filter(item -> item.getRiskReason() != null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toResponse(transaction);
    }

    public TransactionResponse decideFraudAlert(
            Long transactionId,
            FraudDecisionRequest request,
            TransactionPrincipal admin
    ) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .filter(item -> item.getRiskReason() != null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (transaction.getStatus() != TransactionStatus.SUSPECT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette alerte a déjà été traitée.");
        }
        transaction.setStatus(TransactionStatus.valueOf(request.decision().name()));
        transaction.setReviewedBy(admin.userId());
        transaction.setReviewerName(admin.email());
        transaction.setReviewedAt(LocalDateTime.now());
        transaction.setReviewNote(request.note());
        return toResponse(transactionRepository.save(transaction));
    }

    public FraudPolicyResponse getFraudPolicy() {
        return toPolicyResponse(currentFraudPolicy());
    }

    public FraudPolicyResponse updateFraudPolicy(FraudPolicyRequest request) {
        try {
            ZoneId.of(request.timeZone());
        } catch (DateTimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fuseau horaire invalide.");
        }
        FraudPolicy policy = currentFraudPolicy();
        policy.setMaxDailyTransactions(request.maxDailyTransactions());
        policy.setAmountThreshold(request.amountThreshold());
        policy.setTimeZone(request.timeZone());
        policy.setUpdatedAt(LocalDateTime.now());
        return toPolicyResponse(fraudPolicyRepository.save(policy));
    }

    private FraudPolicy currentFraudPolicy() {
        return fraudPolicyRepository.findById(1L).orElseGet(() -> {
            FraudPolicy policy = new FraudPolicy();
            policy.setMaxDailyTransactions(maxDailyTransactions);
            policy.setAmountThreshold(amountThreshold);
            policy.setTimeZone(fraudTimeZone);
            return fraudPolicyRepository.save(policy);
        });
    }

    private FraudPolicyResponse toPolicyResponse(FraudPolicy policy) {
        return new FraudPolicyResponse(
                policy.getMaxDailyTransactions(), policy.getAmountThreshold(),
                policy.getTimeZone(), policy.getUpdatedAt());
    }

        @Scheduled(fixedDelayString = "${fraud.notification.relay-delay-ms:10000}")
        public void relayPendingFraudNotifications() {
        fraudNotificationOutboxRepository
            .findTop50ByDeliveryStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
                List.of(OutboxDeliveryStatus.PENDING, OutboxDeliveryStatus.FAILED),
                maxNotificationAttempts)
            .forEach(this::relayFraudNotification);
        }

        private void relayFraudNotification(FraudNotificationOutbox outbox) {
        outbox.setAttemptCount(outbox.getAttemptCount() + 1);
        outbox.setLastAttemptAt(LocalDateTime.now());
        UserContactResponse owner = outbox.getRecipientEmail() == null || outbox.getRecipientEmail().isBlank()
            ? userClient.getUser(outbox.getOwnerId()) : null;
        if (owner != null) {
            outbox.setRecipientEmail(owner.email());
            if (owner.fullName() != null && !owner.fullName().isBlank()) outbox.setRecipientName(owner.fullName());
        }
        boolean delivered = notificationClient.sendFraudAlert(
            outbox.getOwnerId(), outbox.getAccountId(), outbox.getAccountNumber(),
            outbox.getTransactionId(), outbox.getAmount(), outbox.getRecipientEmail(),
            outbox.getRecipientName(), outbox.getMessage(), outbox.getRiskReason(),
            outbox.getDailyTransactionCount(), outbox.getDailyLimit(), outbox.getOccurredAt());
        outbox.setDeliveryStatus(delivered ? OutboxDeliveryStatus.SENT : OutboxDeliveryStatus.FAILED);
        outbox.setLastError(delivered ? null : "Notification service unavailable or rejected the request");
        fraudNotificationOutboxRepository.save(outbox);
        }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(), t.getAccountId(), t.getType(),
                t.getAmount(), t.getStatus(), t.getDescription(), t.getCreatedAt(),
                "TX-" + t.getId(), t.getRiskReason(), t.getDailyTransactionCount(),
                t.getDailyLimit(), t.getReviewedBy(), t.getReviewedAt(), t.getReviewNote(), t.getReviewerName()
        );
    }
}