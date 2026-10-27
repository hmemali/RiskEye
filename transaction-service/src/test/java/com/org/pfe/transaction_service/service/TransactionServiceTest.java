package com.org.pfe.transaction_service.service;

import com.org.pfe.transaction_service.client.AccountClient;
import com.org.pfe.transaction_service.client.AccountOwnerResponse;
import com.org.pfe.transaction_service.client.NotificationClient;
import com.org.pfe.transaction_service.client.UserClient;
import com.org.pfe.transaction_service.client.UserContactResponse;
import com.org.pfe.transaction_service.dto.CreateTransactionRequest;
import com.org.pfe.transaction_service.dto.TransactionResponse;
import com.org.pfe.transaction_service.entity.FraudReason;
import com.org.pfe.transaction_service.entity.FraudPolicy;
import com.org.pfe.transaction_service.entity.FraudNotificationOutbox;
import com.org.pfe.transaction_service.entity.OutboxDeliveryStatus;
import com.org.pfe.transaction_service.entity.Transaction;
import com.org.pfe.transaction_service.entity.TransactionStatus;
import com.org.pfe.transaction_service.entity.TransactionType;
import com.org.pfe.transaction_service.repository.TransactionRepository;
import com.org.pfe.transaction_service.repository.FraudPolicyRepository;
import com.org.pfe.transaction_service.repository.FraudNotificationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private FraudPolicyRepository fraudPolicyRepository;
    @Mock
    private FraudNotificationOutboxRepository fraudNotificationOutboxRepository;
    @Mock
    private AccountClient accountClient;
    @Mock
    private NotificationClient notificationClient;
    @Mock
    private UserClient userClient;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void configureFraudThresholds() {
        ReflectionTestUtils.setField(transactionService, "maxDailyTransactions", 3);
        ReflectionTestUtils.setField(transactionService, "amountThreshold", new BigDecimal("5000"));
        ReflectionTestUtils.setField(transactionService, "fraudTimeZone", "Africa/Tunis");
        ReflectionTestUtils.setField(transactionService, "maxNotificationAttempts", 10);
        lenient().when(fraudPolicyRepository.findById(1L)).thenReturn(Optional.empty());
        lenient().when(fraudPolicyRepository.save(any(FraudPolicy.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void marksFourthDailyTransactionAcrossClientAccountsAsSuspiciousAndNotifies() {
        AccountOwnerResponse account = account(21L, 7L);
        when(accountClient.getAccount(21L)).thenReturn(account);
        when(accountClient.getAccountsByOwner(7L)).thenReturn(List.of(account(20L, 7L), account));
        when(transactionRepository.countTodayTransactions(
            eq(List.of(20L, 21L)), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(3L);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });
        when(userClient.getUser(7L)).thenReturn(new UserContactResponse(7L, "Client Test", "client@example.test", "CLIENT"));

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId(21L);
        request.setType(TransactionType.RETRAIT);
        request.setAmount(new BigDecimal("20.000"));

        TransactionResponse response = transactionService.createTransaction(request);

        assertEquals(TransactionStatus.SUSPECT, response.getStatus());
        assertEquals(FraudReason.DAILY_TRANSACTION_LIMIT_EXCEEDED, response.getRiskReason());
        assertEquals(4, response.getDailyTransactionCount());
        org.mockito.ArgumentCaptor<FraudNotificationOutbox> outboxCaptor =
            org.mockito.ArgumentCaptor.forClass(FraudNotificationOutbox.class);
        verify(fraudNotificationOutboxRepository).save(outboxCaptor.capture());
        assertEquals(101L, outboxCaptor.getValue().getTransactionId());
        assertEquals("client@example.test", outboxCaptor.getValue().getRecipientEmail());
        assertEquals(4, outboxCaptor.getValue().getDailyTransactionCount());
        org.junit.jupiter.api.Assertions.assertTrue(outboxCaptor.getValue().getMessage().contains("20.000 DT"));
        verify(notificationClient, never()).sendFraudAlert(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any());
    }

    @Test
    void thirdDailyTransactionIsNotSuspicious() {
        AccountOwnerResponse account = account(21L, 7L);
        when(accountClient.getAccount(21L)).thenReturn(account);
        when(accountClient.getAccountsByOwner(7L)).thenReturn(List.of(account));
        when(transactionRepository.countTodayTransactions(
                eq(List.of(21L)), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(2L);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(102L);
            return saved;
        });

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId(21L);
        request.setType(TransactionType.RETRAIT);
        request.setAmount(new BigDecimal("20.000"));

        TransactionResponse response = transactionService.createTransaction(request);

        assertEquals(TransactionStatus.NORMAL, response.getStatus());
        assertEquals(3, response.getDailyTransactionCount());
        org.mockito.Mockito.verifyNoInteractions(notificationClient);
    }

    @Test
    void relaysPendingFraudNotificationAndMarksItSent() {
        FraudNotificationOutbox outbox = new FraudNotificationOutbox();
        outbox.setTransactionId(101L);
        outbox.setOwnerId(7L);
        outbox.setAccountId(21L);
        outbox.setAccountNumber("ACC-21");
        outbox.setAmount(new BigDecimal("20.000"));
        outbox.setRecipientEmail("client@example.test");
        outbox.setRecipientName("Client Test");
        outbox.setMessage("Transaction suspecte");
        outbox.setRiskReason("DAILY_TRANSACTION_LIMIT_EXCEEDED");
        outbox.setDailyTransactionCount(4);
        outbox.setDailyLimit(3);
        outbox.setOccurredAt(LocalDateTime.now());
        when(fraudNotificationOutboxRepository.findTop50ByDeliveryStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
                anyList(), eq(10))).thenReturn(List.of(outbox));
        when(notificationClient.sendFraudAlert(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(true);

        transactionService.relayPendingFraudNotifications();

        assertEquals(OutboxDeliveryStatus.SENT, outbox.getDeliveryStatus());
        assertEquals(1, outbox.getAttemptCount());
    }

    @Test
    void repeatedIdempotencyKeyReturnsTheOriginalTransaction() {
        AccountOwnerResponse account = account(21L, 7L);
        when(accountClient.getAccount(21L)).thenReturn(account);
        when(accountClient.getAccountsByOwner(7L)).thenReturn(List.of(account));
        when(transactionRepository.countTodayTransactions(
                eq(List.of(21L)), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(transactionRepository.findByOwnerIdAndIdempotencyKey(7L, "request-123"))
                .thenReturn(Optional.empty(), Optional.of(Transaction.builder()
                        .id(103L)
                        .accountId(21L)
                        .ownerId(7L)
                        .idempotencyKey("request-123")
                        .type(TransactionType.RETRAIT)
                        .amount(new BigDecimal("20.000"))
                        .status(TransactionStatus.NORMAL)
                        .dailyTransactionCount(1)
                        .dailyLimit(3)
                        .createdAt(LocalDateTime.now())
                        .build()));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(103L);
            return saved;
        });

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId(21L);
        request.setType(TransactionType.RETRAIT);
        request.setAmount(new BigDecimal("20.000"));

        TransactionResponse first = transactionService.createTransaction(request, 7L, "request-123");
        TransactionResponse retry = transactionService.createTransaction(request, 7L, "request-123");

        assertEquals(first.getId(), retry.getId());
        verify(transactionRepository).save(any(Transaction.class));
        verify(fraudNotificationOutboxRepository, never()).save(any(FraudNotificationOutbox.class));
    }

    @Test
    void reusingIdempotencyKeyForDifferentAmountReturnsConflict() {
        when(accountClient.getAccount(21L)).thenReturn(account(21L, 7L));
        when(transactionRepository.findByOwnerIdAndIdempotencyKey(7L, "request-123"))
                .thenReturn(Optional.of(Transaction.builder()
                        .id(103L)
                        .accountId(21L)
                        .ownerId(7L)
                        .idempotencyKey("request-123")
                        .type(TransactionType.RETRAIT)
                        .amount(new BigDecimal("20.000"))
                        .status(TransactionStatus.NORMAL)
                        .build()));

        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAccountId(21L);
        request.setType(TransactionType.RETRAIT);
        request.setAmount(new BigDecimal("25.000"));

        org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
                () -> transactionService.createTransaction(request, 7L, "request-123"));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    private AccountOwnerResponse account(Long accountId, Long ownerId) {
        return new AccountOwnerResponse(
                accountId, "ACC-" + accountId, "COURANT", BigDecimal.ZERO,
                "ACTIVE", ownerId, "Client Test", LocalDateTime.now()
        );
    }
}