package com.org.pfe.notification_service;

import com.org.pfe.notification_service.dto.FraudAlertRequest;
import com.org.pfe.notification_service.dto.NotificationResponse;
import com.org.pfe.notification_service.entity.EmailStatus;
import com.org.pfe.notification_service.repository.NotificationRepository;
import com.org.pfe.notification_service.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentCaptor.forClass;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceApplicationTests {

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private NotificationRepository notificationRepository;

	@MockBean
	private JavaMailSender mailSender;

	@MockBean
	private SimpMessagingTemplate messagingTemplate;

	@BeforeEach
	void cleanNotifications() {
		notificationRepository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void keepsAlertsPrivateAndSupportsReadAndEmailRetry() {
		doThrow(new MailSendException("SMTP unavailable"))
				.doNothing()
				.when(mailSender).send(any(SimpleMailMessage.class));

		FraudAlertRequest request = new FraudAlertRequest();
		request.setOwnerId(42L);
		request.setRecipientEmail("client@example.test");
		request.setRecipientName("Client Test");
		request.setAccountId(12L);
		request.setAccountNumber("RE-1234ABCD");
		request.setTransactionId(99L);
		request.setAmount(new BigDecimal("850.000"));
		request.setRiskReason("DAILY_TRANSACTION_LIMIT_EXCEEDED");
		request.setDailyTransactionCount(4);
		request.setDailyLimit(3);
		request.setMessage("Activité inhabituelle détectée.");

		notificationService.handleFraudAlert(request);

		NotificationResponse first = notificationService.getForUser(42L).get(0);
		assertEquals(EmailStatus.PENDING.name(), first.emailStatus());
		assertEquals(0, first.emailAttempts());
		notificationService.retryPendingEmails();
		first = notificationService.getForUser(42L).get(0);
		assertEquals(EmailStatus.FAILED.name(), first.emailStatus());
		assertEquals(1, first.emailAttempts());
		var capturedEmail = forClass(SimpleMailMessage.class);
		verify(mailSender).send(capturedEmail.capture());
		org.junit.jupiter.api.Assertions.assertTrue(capturedEmail.getValue().getText().contains("Transactions effectuées aujourd'hui : 4 (limite : 3)"));
		org.junit.jupiter.api.Assertions.assertTrue(capturedEmail.getValue().getText().contains("Montant : 850.000 DT"));
		assertEquals(0, notificationService.getForUser(77L).size());
		assertEquals(1, notificationService.getUnreadCount(42L));

		NotificationResponse retried = notificationService.retryEmail(42L, first.id());
		assertEquals(EmailStatus.SENT.name(), retried.emailStatus());
		assertEquals(2, retried.emailAttempts());

		NotificationResponse read = notificationService.markAsRead(42L, first.id());
		assertEquals(true, read.read());
		assertEquals(0, notificationService.getUnreadCount(42L));
	}

}
