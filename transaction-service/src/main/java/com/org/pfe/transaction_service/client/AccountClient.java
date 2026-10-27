package com.org.pfe.transaction_service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AccountClient {

    private final RestTemplate restTemplate;

    @Value("${account.service.api-key:}")
    private String accountServiceApiKey;

    public AccountOwnerResponse getAccount(Long accountId) {
        try {
                return restTemplate.exchange(
                    "http://account-service/api/accounts/" + accountId,
                    HttpMethod.GET, internalRequest(), AccountOwnerResponse.class).getBody();
        } catch (Exception exception) {
            return null;
        }
    }

    public boolean accountExists(Long accountId) {
        return getAccount(accountId) != null;
    }

    public List<AccountOwnerResponse> getAccountsByOwner(Long ownerId) {
        try {
                AccountOwnerResponse[] accounts = restTemplate.exchange(
                    "http://account-service/api/accounts/owner/" + ownerId,
                    HttpMethod.GET, internalRequest(), AccountOwnerResponse[].class).getBody();
            return accounts == null ? List.of() : Arrays.asList(accounts);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private HttpEntity<Void> internalRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Account-Service-Api-Key", accountServiceApiKey);
        return new HttpEntity<>(headers);
    }
}