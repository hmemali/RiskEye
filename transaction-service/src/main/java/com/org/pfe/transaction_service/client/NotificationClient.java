package com.org.pfe.transaction_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    private final RestTemplate restTemplate;

    @Value("${notification.service.api-key:}")
    private String apiKey;

    public boolean sendFraudAlert(
        Long ownerId,
            Long accountId,
        String accountNumber,
        Long transactionId,
        BigDecimal amount,
        String recipientEmail,
        String recipientName,
        String message,
        String riskReason,
        int dailyTransactionCount,
        int dailyLimit,
        LocalDateTime occurredAt
    ) {
        try {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ownerId", ownerId);
        payload.put("recipientEmail", recipientEmail);
        payload.put("recipientName", recipientName);
        payload.put("riskReason", riskReason);
        payload.put("dailyTransactionCount", dailyTransactionCount);
        payload.put("dailyLimit", dailyLimit);
        payload.put("occurredAt", occurredAt);
            payload.put("accountId", accountId);
        payload.put("accountNumber", accountNumber);
        payload.put("transactionId", transactionId);
        payload.put("amount", amount);
        payload.put("message", message);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Notification-Api-Key", apiKey);
        restTemplate.postForObject(
            "http://notification-service/api/notifications/fraud-alert",
            new HttpEntity<>(payload, headers),
            Void.class
        );
            return true;
        } catch (Exception e) {
            log.warn("Could not relay fraud alert for transaction {}", transactionId, e);
            return false;
        }
    }
}