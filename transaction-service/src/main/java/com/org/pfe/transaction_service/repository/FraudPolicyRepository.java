package com.org.pfe.transaction_service.repository;

import com.org.pfe.transaction_service.entity.FraudPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudPolicyRepository extends JpaRepository<FraudPolicy, Long> {
}