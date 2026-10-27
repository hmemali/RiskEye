package com.org.pfe.transaction_service.repository;

import com.org.pfe.transaction_service.entity.FraudNotificationOutbox;
import com.org.pfe.transaction_service.entity.OutboxDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FraudNotificationOutboxRepository extends JpaRepository<FraudNotificationOutbox, Long> {

    List<FraudNotificationOutbox> findTop50ByDeliveryStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
            List<OutboxDeliveryStatus> statuses,
            int maxAttempts
    );
}