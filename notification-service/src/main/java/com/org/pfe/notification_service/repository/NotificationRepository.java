package com.org.pfe.notification_service.repository;

import com.org.pfe.notification_service.entity.Notification;
import com.org.pfe.notification_service.entity.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    Optional<Notification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    Optional<Notification> findByTransactionId(Long transactionId);

    long countByRecipientUserIdAndReadAtIsNull(Long recipientUserId);

    List<Notification> findByEmailStatusInAndEmailAttemptsLessThanOrderByCreatedAtAsc(
            List<EmailStatus> statuses,
            int maxAttempts
    );
}