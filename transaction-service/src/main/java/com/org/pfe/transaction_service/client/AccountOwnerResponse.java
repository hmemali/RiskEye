package com.org.pfe.transaction_service.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountOwnerResponse(
	Long id,
	String accountNumber,
	String accountType,
	BigDecimal balance,
	String status,
	Long ownerId,
	String ownerFullName,
	LocalDateTime createdAt
) {
}