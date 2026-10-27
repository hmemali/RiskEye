package com.org.pfe.transaction_service.repository;

import com.org.pfe.transaction_service.entity.Transaction;
import com.org.pfe.transaction_service.entity.TransactionStatus;
import com.org.pfe.transaction_service.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountId(Long accountId);

    java.util.Optional<Transaction> findByOwnerIdAndIdempotencyKey(Long ownerId, String idempotencyKey);

    List<Transaction> findByAccountIdInOrderByCreatedAtDesc(List<Long> accountIds);

    Page<Transaction> findByAccountIdIn(List<Long> accountIds, Pageable pageable);

    Page<Transaction> findByAccountIdInAndStatus(List<Long> accountIds, TransactionStatus status, Pageable pageable);

        @Query("SELECT t FROM Transaction t WHERE t.accountId IN :accountIds AND (:status IS NULL OR t.status = :status) AND (:type IS NULL OR t.type = :type)")
        Page<Transaction> findClientTransactions(
            @Param("accountIds") List<Long> accountIds,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type,
            Pageable pageable
        );

    List<Transaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.accountId IN :accountIds AND t.createdAt >= :startOfDay AND t.createdAt < :startOfNextDay")
    long countTodayTransactions(
            @Param("accountIds") List<Long> accountIds,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("startOfNextDay") LocalDateTime startOfNextDay
    );
}