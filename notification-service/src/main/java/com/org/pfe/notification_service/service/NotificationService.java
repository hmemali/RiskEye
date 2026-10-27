package com.org.pfe.notification_service.service;

import com.org.pfe.notification_service.dto.FraudAlertRequest;
import com.org.pfe.notification_service.dto.FraudAlertEvent;
import com.org.pfe.notification_service.dto.NotificationResponse;
import com.org.pfe.notification_service.entity.EmailStatus;
import com.org.pfe.notification_service.entity.Notification;
import com.org.pfe.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    @Value("${notification.mail.from:no-reply@riskeye.local}")
    private String fromAddress;

    @Value("${notification.mail.admin-recipient:}")
    private String adminRecipient;

    public void handleFraudAlert(FraudAlertRequest request) {
        if (notificationRepository.findByTransactionId(request.getTransactionId()).isPresent()) return;

        Notification notification = new Notification();
        notification.setRecipientUserId(request.getOwnerId());
        notification.setRecipientEmail(request.getRecipientEmail());
        notification.setRecipientName(request.getRecipientName());
        notification.setAccountId(request.getAccountId());
        notification.setAccountNumber(request.getAccountNumber() == null ? "Non renseigné" : request.getAccountNumber());
        notification.setTransactionId(request.getTransactionId());
        notification.setAmount(request.getAmount());
        notification.setRiskReason(request.getRiskReason());
        notification.setDailyTransactionCount(request.getDailyTransactionCount());
        notification.setDailyLimit(request.getDailyLimit());
        notification.setTransactionOccurredAt(request.getOccurredAt() == null
            ? Instant.now() : request.getOccurredAt().atZone(ZoneId.systemDefault()).toInstant());
        notification.setTitle("Activité inhabituelle détectée");
        notification.setMessage(request.getMessage());
        notification.setCreatedAt(Instant.now());
        notification = notificationRepository.save(notification);

        broadcastAlert(notification);
    }

    public List<NotificationResponse> getForUser(Long userId) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientUserIdAndReadAtIsNull(userId);
    }

    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = getOwnedNotification(userId, notificationId);
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    public NotificationResponse retryEmail(Long userId, Long notificationId) {
        Notification notification = getOwnedNotification(userId, notificationId);
        if (notification.getEmailStatus() == EmailStatus.SENT) return toResponse(notification);
        if (notification.getEmailAttempts() >= 5) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le nombre maximal de tentatives email est atteint.");
        }
        sendClientEmail(notification);
        return toResponse(notificationRepository.findById(notificationId).orElseThrow());
    }

        @org.springframework.scheduling.annotation.Scheduled(
            fixedDelayString = "${notification.mail.retry-delay-ms:60000}")
        public void retryPendingEmails() {
        notificationRepository.findByEmailStatusInAndEmailAttemptsLessThanOrderByCreatedAtAsc(
                Arrays.asList(EmailStatus.PENDING, EmailStatus.FAILED), 5)
            .forEach(notification -> {
                boolean firstDeliveryAttempt = notification.getEmailAttempts() == 0;
                sendClientEmail(notification);
                if (firstDeliveryAttempt) sendAdminEmail(notification);
            });
        }

    private Notification getOwnedNotification(Long userId, Long notificationId) {
        return notificationRepository.findByIdAndRecipientUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void sendClientEmail(Notification notification) {
        notification.setEmailAttempts(notification.getEmailAttempts() + 1);
        notification.setLastEmailAttemptAt(Instant.now());
        if (notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank()) {
            notification.setEmailStatus(EmailStatus.FAILED);
            notificationRepository.save(notification);
            log.warn("Notification {} has no client email recipient", notification.getId());
            return;
        }

        try {
            mailSender.send(emailMessage(notification, notification.getRecipientEmail(), "Alerte de sécurité RiskEye"));
            notification.setEmailStatus(EmailStatus.SENT);
        } catch (RuntimeException exception) {
            notification.setEmailStatus(EmailStatus.FAILED);
            log.error("Could not send notification email {}", notification.getId(), exception);
        }
        notificationRepository.save(notification);
    }

    private void sendAdminEmail(Notification notification) {
        if (adminRecipient == null || adminRecipient.isBlank()) return;
        try {
            mailSender.send(emailMessage(notification, adminRecipient, "Alerte fraude RiskEye"));
        } catch (RuntimeException exception) {
            log.error("Could not send security team alert for notification {}", notification.getId(), exception);
        }
    }

    private SimpleMailMessage emailMessage(Notification notification, String recipient, String subject) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        String transactionTime = notification.getTransactionOccurredAt() == null
                ? "Non renseignée"
                : DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault()).format(notification.getTransactionOccurredAt());
        message.setText(
                "Bonjour " + (notification.getRecipientName() == null || notification.getRecipientName().isBlank()
                        ? "Client" : notification.getRecipientName()) + ",\n\n" +
                        "RiskEye a signalé une transaction pour vérification.\n\n" +
                        "Transactions effectuées aujourd'hui : " + notification.getDailyTransactionCount() +
                        (notification.getDailyLimit() > 0 ? " (limite : " + notification.getDailyLimit() + ")" : "") + "\n" +
                        "Référence : TX-" + notification.getTransactionId() + "\n" +
                        "Compte : " + notification.getAccountNumber() + "\n" +
                        "Montant : " + notification.getAmount() + " DT\n" +
                        "Date : " + transactionTime + "\n" +
                        "Motif : " + riskReasonLabel(notification.getRiskReason()) + "\n\n" +
                        notification.getMessage() + "\n\n" +
                        "Si vous n'êtes pas à l'origine de cette opération, contactez le support RiskEye."
        );
        return message;
    }

    private String riskReasonLabel(String riskReason) {
        if (riskReason == null) return "Activité inhabituelle";
        return switch (riskReason) {
            case "DAILY_TRANSACTION_LIMIT_EXCEEDED" -> "Limite quotidienne de transactions dépassée";
            case "HIGH_VALUE_AMOUNT" -> "Montant supérieur au seuil de surveillance";
            case "MULTIPLE_RISK_SIGNALS" -> "Plusieurs critères de risque détectés";
            default -> "Activité inhabituelle";
        };
    }

    private void broadcastAlert(Notification notification) {
        if (notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank()) return;
        FraudAlertEvent event = new FraudAlertEvent(
                notification.getAccountId(),
                notification.getAccountNumber(),
                notification.getTransactionId(),
                notification.getAmount(),
                notification.getMessage(),
            Instant.now()
        );
        messagingTemplate.convertAndSendToUser(notification.getRecipientEmail(), "/queue/alerts", event);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getRecipientUserId(),
                notification.getAccountId(),
                notification.getAccountNumber(),
                notification.getTransactionId(),
                notification.getAmount(),
                notification.getRiskReason(),
                notification.getDailyTransactionCount(),
                notification.getDailyLimit(),
                notification.getEmailStatus().name(),
                notification.getEmailAttempts(),
                notification.getReadAt() != null,
                notification.getCreatedAt()
        );
    }
}